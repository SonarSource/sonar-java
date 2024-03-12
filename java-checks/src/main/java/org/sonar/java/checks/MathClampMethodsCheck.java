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
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.ExpressionUtils.skipParentheses;

@Rule(key = "S6885")
public class MathClampMethodsCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  public static final String CONDITIONAL_EXPRESSION_MESSAGE = "Use \"Math.clamp\" instead of a conditional expression.";
  public static final String METHOD_INVOCATION_MESSAGE = "Use \"Math.clamp\" instead of \"Math.min\" or \"Math.max\".";

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
    return List.of(Tree.Kind.CONDITIONAL_EXPRESSION, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
      checkConditionalExpression((ConditionalExpressionTree) tree);
    } else { // METHOD_INVOCATION
      checkMethodInvocation((MethodInvocationTree) tree);
    }
  }

  private void checkConditionalExpression(ConditionalExpressionTree firstConditionalExpression) {
    BinaryExpressionTree condition = greaterOrLessBinaryExpression(firstConditionalExpression.condition());
    if (condition != null) {
      boolean isGreater = isGreaterThanOrEqual(condition);
      var trueExpression = skipParentheses(firstConditionalExpression.trueExpression());
      var falseExpression = skipParentheses(firstConditionalExpression.falseExpression());
      if ((shouldReportOnConditional(condition.rightOperand(), trueExpression, falseExpression, isGreater) ||
        shouldReportOnConditional(condition.rightOperand(), falseExpression, trueExpression, !isGreater))
        || (shouldReportOnConditional(condition.leftOperand(), falseExpression, trueExpression, isGreater) ||
          shouldReportOnConditional(condition.leftOperand(), trueExpression, falseExpression, !isGreater))) {
        reportIssue(firstConditionalExpression, CONDITIONAL_EXPRESSION_MESSAGE);
      }
    }
  }

  private static boolean shouldReportOnConditional(ExpressionTree condition, ExpressionTree tree1, ExpressionTree tree2, boolean isMax) {
    if (ExpressionUtils.areVariablesSame(condition, tree1, false)) {
      if (tree2.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
        var innerExpression = (ConditionalExpressionTree) skipParentheses(tree2);
        var innerCondition = (BinaryExpressionTree) skipParentheses(innerExpression.condition());

        return (isLessThanOrEqual(innerExpression.condition())
          && checkInnerExpression(innerCondition, innerExpression.trueExpression(), innerExpression.falseExpression(), isMax))
          || (isGreaterThanOrEqual(innerExpression.condition())
            && checkInnerExpression(innerCondition, innerExpression.trueExpression(), innerExpression.falseExpression(), !isMax));
      } else {
        return matches(isMax ? MATH_MAX_METHOD_MATCHERS : MATH_MIN_METHOD_MATCHERS, tree2);
      }
    }
    return false;
  }

  private static boolean checkInnerExpression(BinaryExpressionTree innerCondition, ExpressionTree innerTrueExpression, ExpressionTree innerFalseExpression, boolean isMax) {
    return isMax
      ? (ExpressionUtils.areVariablesSame(innerCondition.leftOperand(), innerFalseExpression, false)
        && ExpressionUtils.areVariablesSame(innerCondition.rightOperand(), innerTrueExpression, false))
      : (ExpressionUtils.areVariablesSame(innerCondition.leftOperand(), innerTrueExpression, false)
        && ExpressionUtils.areVariablesSame(innerCondition.rightOperand(), innerFalseExpression, false));
  }

  private void checkMethodInvocation(MethodInvocationTree tree) {
    boolean isMinMax = isMin(tree) && isMax(tree.arguments().get(0), tree.arguments().get(1));
    boolean isMaxMin = isMax(tree) && isMin(tree.arguments().get(0), tree.arguments().get(1));
    if (isMinMax || isMaxMin) {
      reportIssue(tree, METHOD_INVOCATION_MESSAGE);
    }
  }

  @CheckForNull
  private static BinaryExpressionTree greaterOrLessBinaryExpression(ExpressionTree tree) {
    ExpressionTree expr = skipParentheses(tree);
    return isGreaterThanOrEqual(expr) || isLessThanOrEqual(expr) ? (BinaryExpressionTree) expr : null;
  }

  private static boolean isGreaterThanOrEqual(ExpressionTree tree) {
    return tree.is(Tree.Kind.GREATER_THAN) || tree.is(Tree.Kind.GREATER_THAN_OR_EQUAL_TO);
  }

  private static boolean isLessThanOrEqual(ExpressionTree tree) {
    return tree.is(Tree.Kind.LESS_THAN) || tree.is(Tree.Kind.LESS_THAN_OR_EQUAL_TO);
  }

  private static boolean isMax(Tree... trees) {
    return matches(MATH_MAX_METHOD_MATCHERS, trees);
  }

  private static boolean isMin(Tree... trees) {
    return matches(MATH_MIN_METHOD_MATCHERS, trees);
  }

  private static boolean matches(MethodMatchers methodMatchers, Tree... trees) {
    for (Tree tree : trees) {
      if (tree.is(Tree.Kind.METHOD_INVOCATION) && methodMatchers.matches((MethodInvocationTree) tree)) {
        return true;
      }
    }
    return false;
  }

}
