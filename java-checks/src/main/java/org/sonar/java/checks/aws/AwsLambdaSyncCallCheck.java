/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks.aws;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.TreeHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.model.JUtils.isLocalVariable;
import static org.sonar.java.model.JUtils.isParameter;

@Rule(key = "S6246")
public class AwsLambdaSyncCallCheck extends AwsReusableResourcesInitializedOnceCheck {

  private static final List<Tree.Kind> NODES_TO_VISIT = List.of(Tree.Kind.METHOD);

  private static final MethodMatchers HANDLE_REQUEST_MATCHER = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("com.amazonaws.services.lambda.runtime.RequestHandler")
      .names("handleRequest")
      .addParametersMatcher("java.lang.Object", "com.amazonaws.services.lambda.runtime.Context")
      .build(),
    MethodMatchers.create()
      .ofSubTypes("com.amazonaws.services.lambda.runtime.RequestStreamHandler")
      .names("handleRequest")
      .addParametersMatcher("java.io.InputStream", "java.io.OutputStream", "com.amazonaws.services.lambda.runtime.Context")
      .build());

  private static final String MESSAGE = "Avoid synchronous calls to other lambdas";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return NODES_TO_VISIT;
  }

  @Override
  public void visitNode(Tree handleRequestMethodTree) {
    var methodTree = (MethodTree) handleRequestMethodTree;
    if (!HANDLE_REQUEST_MATCHER.matches(methodTree)) {
      return;
    }

    var finder = new SyncInvokeFinder();
    methodTree.accept(finder);
    TreeHelper.findReachableMethodsInSameFile(methodTree).forEach(tree -> tree.accept(finder));

    finder.getSyncInvokeCalls().forEach((call, msg) -> reportIssue(ExpressionUtils.methodName(call), msg));
  }

  private static class SyncInvokeFinder extends BaseTreeVisitor {
    private final Map<MethodInvocationTree, String> invokeInvocations = new IdentityHashMap<>();

    private static final MethodMatchers INVOKE_MATCHERS = MethodMatchers.create()
      .ofSubTypes("com.amazonaws.services.lambda.AWSLambda").names("invoke")
      .addParametersMatcher("com.amazonaws.services.lambda.model.InvokeRequest").build();

    private static final MethodMatchers INVOCATIONTYPE_MATCHERS = MethodMatchers.create()
      .ofTypes("com.amazonaws.services.lambda.model.InvokeRequest")
      .names("setInvocationType", "withInvocationType")
      .addParametersMatcher("java.lang.String").build();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      getSyncCalls(tree).ifPresent(msgPart -> invokeInvocations.put(tree, msgPart));
    }

    public Map<MethodInvocationTree, String> getSyncInvokeCalls() {
      return invokeInvocations;
    }

    private static Optional<String> getSyncCalls(MethodInvocationTree tree) {
      if (INVOKE_MATCHERS.matches(tree)) {
        // INVOKE_MATCHER ensures that there is one argument and it is of type IdentifierTree.
        IdentifierTree invokeRequest = (IdentifierTree) tree.arguments().get(0);

        // We know there is at least one usage, i.e. the one we just got above.
        List<IdentifierTree> localUsages = invokeRequest.symbol().usages().stream()
          .filter(u -> isLocalVariable(u.symbol()) && !u.equals(invokeRequest))
          .collect(Collectors.toList());

        if (isParameter(invokeRequest.symbol())
          || localUsages.stream().anyMatch(lu -> isArgumentToACall(lu)
            || statementSetsAsyncCall(lu))
          || declarationSetsAsyncCall(invokeRequest)) {
          return Optional.empty();
        }

        if (hasLocalVarDeclaration(invokeRequest)) {
          return Optional.of(MESSAGE);
        }
        return Optional.empty();
      } else {
        return Optional.empty();
      }
    }

    private static boolean isArgumentToACall(IdentifierTree invokeRequest) {
      return invokeRequest.parent() != null && invokeRequest.parent().is(Tree.Kind.ARGUMENTS);
    }

    private static boolean hasLocalVarDeclaration(IdentifierTree invokeRequest) {
      Tree declaration = invokeRequest.symbol().declaration();
      return (declaration != null && declaration.is(Tree.Kind.VARIABLE) && isLocalVariable(((VariableTree) declaration).symbol()));
    }

    /**
     * Returns true if the statement starting at the identifier 'invokeRequest' sets the InvocationType object
     * to a configuration that sets lambda calls to be async.
     *
     * @param invokeRequest
     * @return true if statement leads calls to lambdas to be async
     */
    private static boolean statementSetsAsyncCall(IdentifierTree invokeRequest) {
      return callChainSetsAsyncCall(invokeRequest);
    }

    /**
     * Returns true if in the call chain starting at 'tree' there is a call that
     * sets 'InvocationType' to async.
     *
     * @param tree an Identifier or a MethodInvocation as starting point of a call chain
     * @return true if InvocationType is set to async
     */
    private static boolean callChainSetsAsyncCall(Tree tree) {
      Tree treeParent = tree.parent();
      if (treeParent != null && treeParent.parent() != null &&
        treeParent.parent().is(Tree.Kind.METHOD_INVOCATION)) {

        MethodInvocationTree methodCall = (MethodInvocationTree) treeParent.parent();

        if (setsInvocationTypeToAsync(methodCall)) {
          return true;
        } else {
          return callChainSetsAsyncCall(methodCall);
        }
      }
      return false;
    }

    /**
     * Returns true if the declaration of the 'invokeRequest' variable leads to async lambda calls.
     *
     * @param invokeRequest the 'invokeRequest' varible being declared
     * @return true if the declaration of the 'invokeRequest' variable leads to async lambda calls
     */
    private static boolean declarationSetsAsyncCall(IdentifierTree invokeRequest) {
      Tree declaration = invokeRequest.symbol().declaration();
      if (declaration != null) {
        AsyncInvocationTypeSetterFinder asyncSetterVisitor = new AsyncInvocationTypeSetterFinder();
        declaration.accept(asyncSetterVisitor);
        return asyncSetterVisitor.found();
      }
      return false;
    }

    private static final class AsyncInvocationTypeSetterFinder extends BaseTreeVisitor {
      private boolean found = false;

      @Override
      public void visitMethodInvocation(MethodInvocationTree methodCall) {
        found = setsInvocationTypeToAsync(methodCall) || found;
      }

      public boolean found() {
        return found;
      }
    }

    private static boolean setsInvocationTypeToAsync(MethodInvocationTree methodCall) {
      Arguments arguments = methodCall.arguments();
      if (INVOCATIONTYPE_MATCHERS.matches(methodCall)) {
        // From the matcher we know there is an argument and it is a string.
        String stringVal = ((LiteralTree) arguments.get(0)).value();
        return (stringVal.equals("\"Event\"") || stringVal.equals("\"DryRun\""));
      }
      return false;
    }
  }
}
