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

import com.google.common.collect.ImmutableList;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S2251",
  priority = Priority.BLOCKER,
  tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.BLOCKER)
public class ForLoopIncrementSignCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ForStatementTree forStatement = (ForStatementTree) tree;
    ExpressionTree condition = forStatement.condition();
    ForLoopIncrement loopIncrement = ForLoopIncrement.findInUpdates(forStatement);
    if (condition == null || loopIncrement == null || loopIncrement.value == null) {
      return;
    }
    checkIncrementSign(condition, loopIncrement);
  }

  private void checkIncrementSign(ExpressionTree condition, ForLoopIncrement loopIncrement) {
    if (condition.is(Tree.Kind.GREATER_THAN, Tree.Kind.GREATER_THAN_OR_EQUAL_TO)) {
      BinaryExpressionTree binaryExp = (BinaryExpressionTree) condition;
      if (loopIncrement.isSameIdentifier(binaryExp.leftOperand())) {
        checkNegativeIncrement(condition, loopIncrement);
      } else if (loopIncrement.isSameIdentifier(binaryExp.rightOperand())) {
        checkPositiveIncrement(condition, loopIncrement);
      }
    } else if (condition.is(Tree.Kind.LESS_THAN, Tree.Kind.LESS_THAN_OR_EQUAL_TO)) {
      BinaryExpressionTree binaryExp = (BinaryExpressionTree) condition;
      if (loopIncrement.isSameIdentifier(binaryExp.leftOperand())) {
        checkPositiveIncrement(condition, loopIncrement);
      } else if (loopIncrement.isSameIdentifier(binaryExp.rightOperand())) {
        checkNegativeIncrement(condition, loopIncrement);
      }
    }
  }

  private void checkPositiveIncrement(Tree tree, ForLoopIncrement loopIncrement) {
    if (loopIncrement.value < 0) {
      addIssue(tree, String.format("\"%s\" is decremented and will never reach \"stop condition\".", loopIncrement.identifier.name()));
    }
  }

  private void checkNegativeIncrement(Tree tree, ForLoopIncrement loopIncrement) {
    if (loopIncrement.value > 0) {
      addIssue(tree, String.format("\"%s\" is incremented and will never reach \"stop condition\".", loopIncrement.identifier.name()));
    }
  }

  private static class ForLoopIncrement {

    private final IdentifierTree identifier;
    private final Integer value;

    public ForLoopIncrement(IdentifierTree identifier, Integer value) {
      this.identifier = identifier;
      this.value = value;
    }

    public boolean isSameIdentifier(ExpressionTree expression) {
      return isSameIdentifier(identifier, expression);
    }

    private static boolean isSameIdentifier(IdentifierTree identifier, ExpressionTree expression) {
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree other = (IdentifierTree) expression;
        return other.name().equals(identifier.name());
      }
      return false;
    }

    @CheckForNull
    public static ForLoopIncrement findInUpdates(ForStatementTree forStatement) {
      ForLoopIncrement result = null;
      List<StatementTree> updates = forStatement.update();
      if (updates.size() == 1) {
        ExpressionStatementTree statement = (ExpressionStatementTree) updates.get(0);
        ExpressionTree expression = statement.expression();
        if (expression.is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.PREFIX_INCREMENT)) {
          UnaryExpressionTree unaryExp = (UnaryExpressionTree) expression;
          result = increment(unaryExp.expression(), 1);
        } else if (expression.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.PREFIX_DECREMENT)) {
          UnaryExpressionTree unaryExp = (UnaryExpressionTree) expression;
          result = increment(unaryExp.expression(), -1);
        } else if (expression.is(Tree.Kind.PLUS_ASSIGNMENT)) {
          AssignmentExpressionTree assignmentExp = (AssignmentExpressionTree) expression;
          result = increment(assignmentExp.variable(), intLiteralValue(assignmentExp.expression()));
        } else if (expression.is(Tree.Kind.MINUS_ASSIGNMENT)) {
          AssignmentExpressionTree assignmentExp = (AssignmentExpressionTree) expression;
          result = increment(assignmentExp.variable(), minus(intLiteralValue(assignmentExp.expression())));
        } else if (expression.is(Tree.Kind.ASSIGNMENT)) {
          AssignmentExpressionTree assignment = (AssignmentExpressionTree) expression;
          result = assignmentIncrement(assignment);
        }
      }
      return result;
    }

    @CheckForNull
    private static ForLoopIncrement increment(ExpressionTree expression, Integer value) {
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        return new ForLoopIncrement((IdentifierTree) expression, value);
      }
      return null;
    }

    @CheckForNull
    private static Integer intLiteralValue(ExpressionTree expression) {
      if (expression.is(Tree.Kind.INT_LITERAL)) {
        return intLiteralValue((LiteralTree) expression);
      }
      if (expression.is(Tree.Kind.UNARY_MINUS, Tree.Kind.UNARY_PLUS)) {
        UnaryExpressionTree unaryExp = (UnaryExpressionTree) expression;
        Integer subExpressionIntValue = intLiteralValue(unaryExp.expression());
        return expression.is(Tree.Kind.UNARY_MINUS) ? minus(subExpressionIntValue) : subExpressionIntValue;
      }
      return null;
    }

    private static Integer intLiteralValue(LiteralTree literal) {
      return Integer.valueOf(literal.value());
    }

    private static Integer minus(@Nullable Integer nullableInteger) {
      return nullableInteger == null ? null : -nullableInteger;
    }

    private static ForLoopIncrement assignmentIncrement(AssignmentExpressionTree assignmentExpression) {
      ExpressionTree expression = assignmentExpression.expression();
      ExpressionTree variable = assignmentExpression.variable();
      if (variable.is(Tree.Kind.IDENTIFIER) && expression.is(Tree.Kind.PLUS, Tree.Kind.MINUS)) {
        BinaryExpressionTree binaryExp = (BinaryExpressionTree) expression;
        Integer increment = intLiteralValue(binaryExp.rightOperand());
        if (increment != null && isSameIdentifier((IdentifierTree) variable, binaryExp.leftOperand())) {
          increment = expression.is(Tree.Kind.MINUS) ? minus(increment) : increment;
          return increment(variable, increment);
        }
      }
      return null;
    }

  }

}
