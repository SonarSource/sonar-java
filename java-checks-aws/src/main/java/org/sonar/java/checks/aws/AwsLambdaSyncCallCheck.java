/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6246")
public class AwsLambdaSyncCallCheck extends AbstractAwsMethodVisitor {

  private static final String MESSAGE = "Avoid synchronous calls to other lambdas.";

  @Override
  void visitReachableMethodsFromHandleRequest(Set<MethodTree> methodTrees) {
    var finder = new SyncInvokeFinder();
    methodTrees.forEach(m -> m.accept(finder));

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

        // INVOKE_MATCHER ensures that there is one argument.
        ExpressionTree argument = tree.arguments().get(0);

        if (!argument.is(Tree.Kind.IDENTIFIER)) {
          return Optional.empty();
        }
        IdentifierTree invokeRequest = (IdentifierTree) argument;

        // We know there is at least one usage, i.e. the one we just got above.
        List<IdentifierTree> localUsages = invokeRequest.symbol().usages().stream()
          .filter(u -> u.symbol().isLocalVariable() && !u.equals(invokeRequest))
          .collect(Collectors.toList());

        if (invokeRequest.symbol().isParameter()
          || localUsages.stream().anyMatch(lu -> isArgumentToACall(lu) || statementSetsAsyncCall(lu))
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
      return (declaration != null && declaration.is(Tree.Kind.VARIABLE) && ((VariableTree) declaration).symbol().isLocalVariable());
    }

    /**
     * Returns true if the statement starting at 'tree' sets the InvocationType object
     * to a configuration that leads to async lambda calls. The statement could be a sequence of calls.
     *
     * @param tree the statement starting point
     * @return true if statement leads lambda calls to be async
     */
    private static boolean statementSetsAsyncCall(Tree tree) {
      MethodInvocationTree methodCall = findMethodInvocationTreeAncestor(tree);
      return (methodCall != null && (setsInvocationTypeToAsync(methodCall) || statementSetsAsyncCall(methodCall)));
    }

    private static MethodInvocationTree findMethodInvocationTreeAncestor(Tree tree) {
      Tree parent = tree.parent();
      while (parent != null) {
        if (parent.is(Tree.Kind.METHOD_INVOCATION)) {
          return (MethodInvocationTree) parent;
        }
        parent = parent.parent();
      }
      return null;
    }

    /**
     * Returns true if the declaration of the 'invokeRequest' variable leads to async lambda calls.
     *
     * @param invokeRequest the 'invokeRequest' varible being declared
     * @return true if the declaration of the 'invokeRequest' variable leads to async lambda calls
     */
    private static boolean declarationSetsAsyncCall(IdentifierTree invokeRequest) {
      Tree declaration = invokeRequest.symbol().declaration();
      if (declaration == null) {
        // Declaration not found so we can't say that calls are sync.
        // E.g. decalaration coming from another file.
        return true;
      }
      AsyncInvocationTypeSetterFinder asyncSetterVisitor = new AsyncInvocationTypeSetterFinder();
      declaration.accept(asyncSetterVisitor);
      return asyncSetterVisitor.found();
    }

    private static final class AsyncInvocationTypeSetterFinder extends BaseTreeVisitor {
      private boolean found = false;

      @Override
      public void visitMethodInvocation(MethodInvocationTree methodCall) {
        found = found || setsInvocationTypeToAsync(methodCall);
      }

      public boolean found() {
        return found;
      }
    }

    private static boolean setsInvocationTypeToAsync(MethodInvocationTree methodCall) {
      Arguments arguments = methodCall.arguments();
      if (INVOCATIONTYPE_MATCHERS.matches(methodCall)) {
        // From the matcher we know there is an argument and it is a string.
        String stringVal = ExpressionsHelper.getConstantValueAsString(arguments.get(0)).value();
        return "Event".equals(stringVal)
          || "DryRun".equals(stringVal)
          // Could not get the string real value, therefore sync calls are out of the picture.
          || stringVal == null;
      }
      return false;
    }
  }
}
