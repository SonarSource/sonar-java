/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S2252")
public class ForLoopFalseConditionCheck extends AbstractForLoopRule {

  @Override
  public void visitForStatement(ForStatementTree forStatement) {
    ExpressionTree condition = forStatement.condition();
    if (condition != null && (isAlwaysFalseCondition(condition) || isConditionFalseAtInitialization(forStatement))) {
      reportIssue(condition, "This loop will never execute.");
    }
  }

  private static boolean isAlwaysFalseCondition(ExpressionTree expression) {
    if (expression.is(Tree.Kind.BOOLEAN_LITERAL)) {
      return Boolean.FALSE.equals(booleanLiteralValue(expression));
    }
    if (expression.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
      ExpressionTree subExpression = ((UnaryExpressionTree) expression).expression();
      return Boolean.TRUE.equals(booleanLiteralValue(subExpression));
    }
    return false;
  }

  @CheckForNull
  private static Boolean booleanLiteralValue(ExpressionTree expression) {
    if (expression.is(Tree.Kind.BOOLEAN_LITERAL)) {
      return Boolean.valueOf(((LiteralTree) expression).value());
    }
    return null;
  }

  private static boolean isConditionFalseAtInitialization(ForStatementTree forStatement) {
    Iterable<ForLoopInitializer> initializers = ForLoopInitializer.list(forStatement);
    ExpressionTree condition = forStatement.condition();
    if (!condition.is(Tree.Kind.GREATER_THAN, Tree.Kind.GREATER_THAN_OR_EQUAL_TO, Tree.Kind.LESS_THAN, Tree.Kind.LESS_THAN_OR_EQUAL_TO)) {
      return false;
    }
    BinaryExpressionTree binaryCondition = (BinaryExpressionTree) condition;
    Integer leftOperand = eval(binaryCondition.leftOperand(), initializers);
    Integer rightOperand = eval(binaryCondition.rightOperand(), initializers);
    if (leftOperand != null && rightOperand != null) {
      return !evaluateCondition(condition, leftOperand, rightOperand);
    }
    return false;
  }

  private static boolean evaluateCondition(ExpressionTree condition, int leftOperand, int rightOperand) {
    boolean conditionValue;
    switch (condition.kind()) {
      case GREATER_THAN:
        conditionValue = leftOperand > rightOperand;
        break;
      case GREATER_THAN_OR_EQUAL_TO:
        conditionValue = leftOperand >= rightOperand;
        break;
      case LESS_THAN:
        conditionValue = leftOperand < rightOperand;
        break;
      case LESS_THAN_OR_EQUAL_TO:
        conditionValue = leftOperand <= rightOperand;
        break;
      default:
        conditionValue = true;
    }
    return conditionValue;
  }

  private static Integer eval(ExpressionTree expression, Iterable<ForLoopInitializer> initializers) {
    Object resolvedConstant = expression.asConstant().orElse(null);
    Integer intLiteralValue = resolvedConstant instanceof Integer integer ? integer : null;
    if (intLiteralValue == null) {
      for (ForLoopInitializer initializer : initializers) {
        if (initializer.hasSameIdentifier(expression)) {
          intLiteralValue = initializer.value();
        }
      }
    }
    return intLiteralValue;
  }

}
