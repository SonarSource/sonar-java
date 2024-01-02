/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.tests;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import static org.sonar.java.checks.helpers.UnitTestUtils.FAIL_METHOD_MATCHER;

public abstract class AbstractOneExpectedExceptionRule extends IssuableSubscriptionVisitor {

  private static final String JUNIT4_ASSERT = "org.junit.Assert";
  private static final String ASSERTJ_ASSERTIONS = "org.assertj.core.api.Assertions";

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

  private static final MethodMatchers ASSERTJ_CATCH_THROWABLE_OF_TYPE = MethodMatchers.create()
    .ofTypes(ASSERTJ_ASSERTIONS)
    .names("catchThrowableOfType")
    .addParametersMatcher("org.assertj.core.api.ThrowableAssert$ThrowingCallable", "java.lang.Class")
    .build();

  private static final MethodMatchers ASSERTJ_ASSERT_THAT_EXCEPTION_OF_TYPE = MethodMatchers.create()
    .ofTypes(ASSERTJ_ASSERTIONS)
    .names("assertThatExceptionOfType")
    .addParametersMatcher("java.lang.Class")
    .build();

  private static final MethodMatchers ASSERTJ_IS_THROWN_BY = MethodMatchers.create()
    .ofTypes("org.assertj.core.api.ThrowableTypeAssert")
    .names("isThrownBy")
    .addParametersMatcher("org.assertj.core.api.ThrowableAssert$ThrowingCallable")
    .build();

  private static final MethodMatchers ASSERTJ_ASSERT_CODE = MethodMatchers.create()
    .ofTypes(ASSERTJ_ASSERTIONS)
    .names("assertThatCode", "assertThatThrownBy")
    .withAnyParameters()
    .build();

  private static final MethodMatchers ASSERTJ_INSTANCE_OF_PREDICATES = MethodMatchers.create()
    .ofSubTypes("org.assertj.core.api.Assert")
    .names("isInstanceOf", "isExactlyInstanceOf", "isOfAnyClassIn", "isInstanceOfAny")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.TRY_STATEMENT, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      visitMethodInvocation((MethodInvocationTree) tree);
    } else {
      visitTryStatement((TryStatementTree) tree);
    }
  }

  private void visitMethodInvocation(MethodInvocationTree mit) {
    Arguments arguments = mit.arguments();
    if (arguments.isEmpty()) {
      return;
    }
    IdentifierTree identifierTree = ExpressionUtils.methodName(mit);
    if (ASSERTJ_CATCH_THROWABLE_OF_TYPE.matches(mit)) {
      processAssertThrowsArguments(identifierTree, arguments.get(1), arguments.get(0));
    } else if (ASSERTJ_ASSERT_CODE.matches(mit)) {
      MethodTreeUtils.subsequentMethodInvocation(mit, ASSERTJ_INSTANCE_OF_PREDICATES)
        .ifPresent(isInstanceOf -> processAssertThrowsArguments(identifierTree, isInstanceOf.arguments(), arguments.get(0)));
    } else if (ASSERTJ_ASSERT_THAT_EXCEPTION_OF_TYPE.matches(mit)) {
      MethodTreeUtils.subsequentMethodInvocation(mit, ASSERTJ_IS_THROWN_BY)
        .ifPresent(isThrownBy -> processAssertThrowsArguments(ExpressionUtils.methodName(isThrownBy), arguments.get(0), isThrownBy.arguments().get(0)));
    } else if (JUNIT4_ASSERT_THROWS_WITH_MESSAGE.matches(mit)) {
      processAssertThrowsArguments(identifierTree, arguments.get(1), arguments.get(2));
    } else if (arguments.size() >= 2 && ALL_ASSERT_THROWS_MATCHER.matches(mit)) {
      processAssertThrowsArguments(identifierTree, arguments.get(0), arguments.get(1));
    }
  }

  private void visitTryStatement(TryStatementTree tryStatementTree) {
    if (isTryCatchFail(tryStatementTree)) {
      List<Type> expectedTypes = tryStatementTree.catches().stream().map(c -> c.parameter().type().symbolType()).collect(Collectors.toList());
      reportMultipleCallInTree(expectedTypes, tryStatementTree.block(), tryStatementTree.tryKeyword(), "body of this try/catch");
    }
  }

  private void processAssertThrowsArguments(Tree reportLocation, ExpressionTree expectedType, ExpressionTree executable) {
    processAssertThrowsArguments(reportLocation, Collections.singletonList(expectedType), executable);
  }

  private void processAssertThrowsArguments(Tree reportLocation, List<ExpressionTree> expectedTypes, ExpressionTree executable) {
    if (!expectedTypes.isEmpty() && executable.is(Tree.Kind.LAMBDA_EXPRESSION)) {
      List<Type> expectedExceptions = expectedTypes.stream()
        .map(AbstractOneExpectedExceptionRule::getExpectedException)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(ExpressionTree::symbolType)
        .collect(Collectors.toList());

      if (!expectedExceptions.isEmpty()) {
        Tree lambda = ((LambdaExpressionTree) executable).body();
        reportMultipleCallInTree(expectedExceptions, lambda, reportLocation, "code of the lambda");
      }
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

  private static boolean isTryCatchFail(TryStatementTree tree) {
    List<StatementTree> statementTrees = tree.block().body();
    if (!statementTrees.isEmpty()) {
      StatementTree lastElement = statementTrees.get(statementTrees.size() - 1);
      if (lastElement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
        ExpressionTree expressionTree = ((ExpressionStatementTree) lastElement).expression();
        if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
          return FAIL_METHOD_MATCHER.matches((MethodInvocationTree) expressionTree);
        }
      }
    }
    return false;
  }

  abstract void reportMultipleCallInTree(List<Type> expectedExceptions, Tree treeToVisit, Tree reportLocation, String placeToRefactor);

  static boolean isChecked(Type type) {
    return !type.isSubtypeOf("java.lang.RuntimeException") && !type.isSubtypeOf("java.lang.Error");
  }

  static List<JavaFileScannerContext.Location> secondaryLocations(List<Tree> methodInvocationTrees, String message) {
    return methodInvocationTrees.stream()
      .map(expr -> new JavaFileScannerContext.Location(message, expr))
      .collect(Collectors.toList());
  }

}
