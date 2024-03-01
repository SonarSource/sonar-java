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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6885")
public class MathClampMethodsCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  public static final String CONDITIONAL_EXPRESSION_MESSAGE = "Use \"Math.clamp\" instead of a conditional expression.";
  public static final String METHOD_INVOCATION_MESSAGE = "Use \"Math.clamp\" instead of \"Math.min\" or \"Math.max\".";
  public static final String IF_ELSE_STATEMENT_MESSAGE = "Use \"Math.clamp\" instead of an if-else statement.";

  public static final String JAVA_LANG_MATH = "java.lang.Math";
  private static final MethodMatchers MATH_MIN_METHOD_MATCHERS = MethodMatchers.create()
    .ofTypes(JAVA_LANG_MATH)
    .names("min")
    .withAnyParameters()
    .build();

  private static final MethodMatchers MATH_MAX_METHOD_MATCHERS = MethodMatchers.create()
    .ofTypes(JAVA_LANG_MATH)
    .names("max")
    .withAnyParameters()
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CONDITIONAL_EXPRESSION, Tree.Kind.METHOD_INVOCATION, Tree.Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
      checkConditionalExpression((ConditionalExpressionTree) tree);
    }
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      checkMethodInvocation((MethodInvocationTree) tree);
    }
    if (tree.is(Tree.Kind.IF_STATEMENT)) {
      checkIfStatement((IfStatementTree) tree);
    }
  }

  private void checkConditionalExpression(ConditionalExpressionTree tree) {
    if (isGreaterThanOrEqual(tree.condition())) {
      var condition = (BinaryExpressionTree) tree.condition();
      var trueExpression = ExpressionUtils.skipParentheses(tree.trueExpression());
      var falseExpression = ExpressionUtils.skipParentheses(tree.falseExpression());

      if (shouldReportOnConditional(condition.rightOperand(), trueExpression, falseExpression, MATH_MAX_METHOD_MATCHERS, true)) {
        reportIssue(tree, CONDITIONAL_EXPRESSION_MESSAGE);
      }

      if (shouldReportOnConditional(condition.rightOperand(), falseExpression, trueExpression, MATH_MIN_METHOD_MATCHERS, false)) {
        reportIssue(tree, CONDITIONAL_EXPRESSION_MESSAGE);
      }
    }
    if (isLessThanOrEqual(tree.condition())) {
      var condition = (BinaryExpressionTree) tree.condition();
      var trueExpression = tree.trueExpression();
      var falseExpression = tree.falseExpression();

      if (shouldReportOnConditional(condition.rightOperand(), trueExpression, falseExpression, MATH_MIN_METHOD_MATCHERS, false)) {
        reportIssue(tree, CONDITIONAL_EXPRESSION_MESSAGE);
      }

      if (shouldReportOnConditional(condition.rightOperand(), falseExpression, trueExpression, MATH_MAX_METHOD_MATCHERS, true)) {
        reportIssue(tree, CONDITIONAL_EXPRESSION_MESSAGE);
      }
    }
  }

  private static boolean shouldReportOnConditional(ExpressionTree condition, ExpressionTree tree1, ExpressionTree tree2, MethodMatchers matcher, boolean isMax) {
    if (condition.is(Tree.Kind.IDENTIFIER) && tree1.is(Tree.Kind.IDENTIFIER) && (((IdentifierTree) condition).name().equals(((IdentifierTree) tree1).name()))) {
      if (tree2.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
        var innerExpression = (ConditionalExpressionTree) ExpressionUtils.skipParentheses(tree2);
        var innerCondition = (BinaryExpressionTree) ExpressionUtils.skipParentheses(innerExpression.condition());

        return (isLessThanOrEqual(innerExpression.condition())
          && checkInnerExpression(innerCondition, innerExpression.trueExpression(), innerExpression.falseExpression(), isMax))
          || (isGreaterThanOrEqual(innerExpression.condition())
            && checkInnerExpression(innerCondition, innerExpression.trueExpression(), innerExpression.falseExpression(), !isMax));
      } else {
        return tree2.is(Tree.Kind.METHOD_INVOCATION) && (matcher.matches((MethodInvocationTree) tree2));
      }
    }
    return false;
  }

  private static boolean checkInnerExpression(BinaryExpressionTree innerCondition, ExpressionTree innerTrueExpression, ExpressionTree innerFalseExpression, boolean isMax) {
    if (innerCondition.leftOperand().is(Tree.Kind.IDENTIFIER) && innerCondition.rightOperand().is(Tree.Kind.IDENTIFIER)
      && innerTrueExpression.is(Tree.Kind.IDENTIFIER) && innerFalseExpression.is(Tree.Kind.IDENTIFIER)) {
      var leftOperandName = ((IdentifierTree) innerCondition.leftOperand()).name();
      var rightOperandName = ((IdentifierTree) innerCondition.rightOperand()).name();
      var trueExpressionName = ((IdentifierTree) innerTrueExpression).name();
      var falseExpressionName = ((IdentifierTree) innerFalseExpression).name();

      return isMax ? (leftOperandName.equals(falseExpressionName) && rightOperandName.equals(trueExpressionName))
        : (leftOperandName.equals(trueExpressionName) && rightOperandName.equals(falseExpressionName));
    }
    return false;
  }

  private void checkMethodInvocation(MethodInvocationTree tree) {
    if (MATH_MIN_METHOD_MATCHERS.matches(tree)) {
      var firstArg = tree.arguments().get(0);
      var secondArg = tree.arguments().get(1);

      if ((firstArg.is(Tree.Kind.METHOD_INVOCATION) && MATH_MAX_METHOD_MATCHERS.matches((MethodInvocationTree) firstArg)) ||
        (secondArg.is(Tree.Kind.METHOD_INVOCATION) && MATH_MAX_METHOD_MATCHERS.matches((MethodInvocationTree) secondArg))) {
        reportIssue(tree, METHOD_INVOCATION_MESSAGE);
      }
    }
    if (MATH_MAX_METHOD_MATCHERS.matches(tree)) {
      var firstArg = tree.arguments().get(0);
      var secondArg = tree.arguments().get(1);

      if ((firstArg.is(Tree.Kind.METHOD_INVOCATION) && MATH_MIN_METHOD_MATCHERS.matches((MethodInvocationTree) firstArg)) ||
        (secondArg.is(Tree.Kind.METHOD_INVOCATION) && MATH_MIN_METHOD_MATCHERS.matches((MethodInvocationTree) secondArg))) {
        reportIssue(tree, METHOD_INVOCATION_MESSAGE);
      }
    }
  }

  private void checkIfStatement(IfStatementTree tree) {
    if (isGreaterThanOrLessThan(tree.condition())
      && (tree.elseStatement() != null && tree.elseStatement().is(Tree.Kind.IF_STATEMENT))) {
      var elseIfStatement = (IfStatementTree) tree.elseStatement();
      if (isGreaterThanOrLessThan(elseIfStatement.condition())) {
        reportIssue(tree, IF_ELSE_STATEMENT_MESSAGE);
      }
    }
  }

  private static boolean isGreaterThanOrEqual(ExpressionTree tree) {
    return tree.is(Tree.Kind.GREATER_THAN) || tree.is(Tree.Kind.GREATER_THAN_OR_EQUAL_TO);
  }

  private static boolean isLessThanOrEqual(ExpressionTree tree) {
    return tree.is(Tree.Kind.LESS_THAN) || tree.is(Tree.Kind.LESS_THAN_OR_EQUAL_TO);
  }

  private static boolean isGreaterThanOrLessThan(Tree tree) {
    return tree.is(Tree.Kind.GREATER_THAN) || tree.is(Tree.Kind.LESS_THAN);
  }

}
