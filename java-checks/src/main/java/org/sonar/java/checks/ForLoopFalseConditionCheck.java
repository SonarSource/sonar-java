/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.apache.commons.lang.BooleanUtils;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import javax.annotation.CheckForNull;

@Rule(
  key = "S2252",
  priority = Priority.CRITICAL,
  tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class ForLoopFalseConditionCheck extends AbstractForLoopRule {

  @Override
  public void visitForStatement(ForStatementTree forStatement) {
    ExpressionTree condition = forStatement.condition();
    if (condition != null && (isAlwaysFalseCondition(condition) || isConditionFalseAtInitialization(forStatement))) {
      addIssue(condition, "This loop will never execute.");
    }
  }

  private boolean isAlwaysFalseCondition(ExpressionTree expression) {
    if (expression.is(Tree.Kind.BOOLEAN_LITERAL)) {
      return BooleanUtils.isFalse(booleanLiteralValue(expression));
    }
    if (expression.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
      ExpressionTree subExpression = ((UnaryExpressionTree) expression).expression();
      return BooleanUtils.isTrue(booleanLiteralValue(subExpression));
    }
    return false;
  }

  @CheckForNull
  private Boolean booleanLiteralValue(ExpressionTree expression) {
    if (expression.is(Tree.Kind.BOOLEAN_LITERAL)) {
      return Boolean.valueOf(((LiteralTree) expression).value());
    }
    return null;
  }

  private boolean isConditionFalseAtInitialization(ForStatementTree forStatement) {
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

  private boolean evaluateCondition(ExpressionTree condition, int leftOperand, int rightOperand) {
    boolean conditionValue = true;
    if (condition.is(Tree.Kind.GREATER_THAN)) {
      conditionValue = leftOperand > rightOperand;
    } else if (condition.is(Tree.Kind.GREATER_THAN_OR_EQUAL_TO)) {
      conditionValue = leftOperand >= rightOperand;
    } else if (condition.is(Tree.Kind.LESS_THAN)) {
      conditionValue = leftOperand < rightOperand;
    } else if (condition.is(Tree.Kind.LESS_THAN_OR_EQUAL_TO)) {
      conditionValue = leftOperand <= rightOperand;
    }
    return conditionValue;
  }

  private Integer eval(ExpressionTree expression, Iterable<ForLoopInitializer> initializers) {
    Integer intLiteralValue = intLiteralValue(expression);
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
