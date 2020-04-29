/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

@Rule(key = "S5783")
public class OneExpectedCheckExceptionCheck extends IssuableSubscriptionVisitor {

  private static final String JUNIT4_ASSERT = "org.junit.Assert";

  private static final MethodMatchers JUNIT4_ASSERT_THROWS_WITH_MESSAGE = MethodMatchers.create()
    .ofTypes(JUNIT4_ASSERT)
    .names("assertThrows")
    .addParametersMatcher("java.lang.String", MethodMatchers.ANY, MethodMatchers.ANY)
    .build();

  private static final MethodMatchers ALL_ASSERT_THROWS_MATCHER = MethodMatchers.create()
    .ofTypes(JUNIT4_ASSERT, "org.junit.jupiter.api.Assertions")
    .names("assertThrows")
    .withAnyParameters()
    .build();

  private static final MethodMatchers JUNIT_FAIL_MATCHER = MethodMatchers.create()
    .ofTypes(JUNIT4_ASSERT, "org.junit.jupiter.api.Assertions")
    .names("fail")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.TRY_STATEMENT, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) tree;
      Arguments arguments = mit.arguments();
      if (JUNIT4_ASSERT_THROWS_WITH_MESSAGE.matches(mit)) {
        processAssertThrowsArguments(arguments.get(1), arguments.get(2));
      } else if (arguments.size() >= 2 && ALL_ASSERT_THROWS_MATCHER.matches(mit)) {
        processAssertThrowsArguments(arguments.get(0), arguments.get(1));
      }
    } else {
      TryStatementTree tryStatementTree = (TryStatementTree) tree;
      if (isTryCatchIdiom(tryStatementTree)) {
        tryStatementTree.catches().forEach(c ->
          reportMultipleCallThrowingExceptionInTree(c.parameter().type().symbolType(), tryStatementTree.block(), c.parameter().type())
        );
      }
    }
  }

  private void processAssertThrowsArguments(ExpressionTree expectedType, ExpressionTree executable) {
    Optional<IdentifierTree> expectedException = getExpectedException(expectedType);
    if (expectedException.isPresent() && executable.is(Tree.Kind.LAMBDA_EXPRESSION)) {
      IdentifierTree expectedIdentifier = expectedException.get();
      reportMultipleCallThrowingExceptionInTree(expectedIdentifier.symbolType(), ((LambdaExpressionTree) executable).body(), expectedIdentifier);
    }
  }

  private static Optional<IdentifierTree> getExpectedException(ExpressionTree expectedType) {
    if (expectedType.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = ((MemberSelectExpressionTree) expectedType);
      ExpressionTree expression = memberSelect.expression();
      if ("class".equals(memberSelect.identifier().name()) && expression.is(Tree.Kind.IDENTIFIER)) {
        return Optional.of((IdentifierTree) expression);
      }
    }
    return Optional.empty();
  }

  private static boolean isTryCatchIdiom(TryStatementTree tree) {
    List<StatementTree> statementTrees = tree.block().body();
    if (!statementTrees.isEmpty()) {
      StatementTree lastElement = statementTrees.get(statementTrees.size() - 1);
      if (lastElement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
        ExpressionTree expressionTree = ((ExpressionStatementTree) lastElement).expression();
        if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
          return JUNIT_FAIL_MATCHER.matches((MethodInvocationTree) expressionTree);
        }
      }
    }
    return false;
  }

  private void reportMultipleCallThrowingExceptionInTree(Type expectedException, Tree tree, Tree reportLocation) {
    MethodInvocationThrowing visitor = new MethodInvocationThrowing(expectedException); //m.symbolType().isSubtypeOf(expectedType)
    tree.accept(visitor);
    List<MethodInvocationTree> methodInvocationTrees = visitor.methodInvocationTrees;
    if (methodInvocationTrees.size() > 1) {
      reportIssue(reportLocation,
        "The tested checked exception can be raised from multiples call, it is unclear what is really tested.",
        secondaryLocations(methodInvocationTrees),
        null);
    }
  }

  private static List<JavaFileScannerContext.Location> secondaryLocations(List<MethodInvocationTree> methodInvocationTrees) {
    return methodInvocationTrees.stream()
      .map(ExpressionUtils::methodName)
      .map(expr -> new JavaFileScannerContext.Location("Method call", expr))
      .collect(Collectors.toList());
  }

  private static class MethodInvocationThrowing extends BaseTreeVisitor {
    List<MethodInvocationTree> methodInvocationTrees = new ArrayList<>();
    private final Type expectedException;

    MethodInvocationThrowing(Type expectedException) {
      this.expectedException = expectedException;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      Symbol symbol = mit.symbol();

      if (symbol.isMethodSymbol() && ((Symbol.MethodSymbol) symbol).thrownTypes().stream().anyMatch(t -> t.isSubtypeOf(expectedException))) {
        methodInvocationTrees.add(mit);
      }
      super.visitMethodInvocation(mit);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Skip class
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // Skip lambdas
    }
  }

}
