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
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.model.JUtils.isLocalVariable;
import static org.sonar.java.model.JUtils.isParameter;

@Rule(key = "S6243")
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

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      getSynCalls(tree).ifPresent(msgPart -> invokeInvocations.put(tree, msgPart));
    }

    // TODO ask: this is for sdk 1 only. How to deal with v2?
    // TODO extract Johann's code
    private static Optional<String> getSynCalls(MethodInvocationTree tree) {
      if (INVOKE_MATCHERS.matches(tree)) {
        // INVOKE_MATCHER implies there is one argument and it is of type IdentifierTree.
        IdentifierTree invokeRequest = (IdentifierTree) tree.arguments().get(0);

        if (isParameter(invokeRequest.symbol())) {
          return Optional.empty();
        }

        // We know there is at least one usage, i.e. the one we just got above.
        List<IdentifierTree> localUsages = invokeRequest.symbol().usages().stream()
          .filter(u -> isLocalVariable(u.symbol()) && !u.equals(invokeRequest))
          .collect(Collectors.toList());

        if (localUsages.stream().anyMatch(lu -> isArgumentToACall(lu) ||
          setsInvocationTypeToEvent(lu))) {
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
      return invokeRequest.parent().is(Tree.Kind.ARGUMENTS);
    }

    private static boolean hasLocalVarDeclaration(IdentifierTree invokeRequest) {
      Tree declaration = invokeRequest.symbol().declaration();
      return (declaration != null && declaration.is(Tree.Kind.VARIABLE) && isLocalVariable(((VariableTree) declaration).symbol()));
    }

    private static boolean setsInvocationTypeToEvent(IdentifierTree invokeRequest) {
      if (invokeRequest.parent() != null && invokeRequest.parent().parent().is(Tree.Kind.METHOD_INVOCATION)) {
        MethodMatchers INVOCATIONTYPE_MATCHERS = MethodMatchers.create()
          .ofTypes("com.amazonaws.services.lambda.model.InvokeRequest")
          .names("setInvocationType", "withInvocationType")
          .addParametersMatcher("java.lang.String").build();

        MethodInvocationTree methodCall = (MethodInvocationTree) invokeRequest.parent().parent();

        if (INVOCATIONTYPE_MATCHERS.matches(methodCall)) {
          ExpressionTree argument = methodCall.arguments().get(0);
          if (argument.is(Tree.Kind.STRING_LITERAL)) {
            String stringVal = ((LiteralTree) argument).value();
            // TODO: ask why this is so
            return stringVal.equals("\"Event\"");
          }
        }
      }
      return false;
    }

    public Map<MethodInvocationTree, String> getSyncInvokeCalls() {
      return invokeInvocations;
    }
  }
}
