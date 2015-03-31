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
package org.sonar.java.se;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

public class ExpressionEvaluatorVisitor extends BaseTreeVisitor {

  public SymbolicValue evaluate(ExecutionState state, Tree tree) {
    currentConditionalState = new ConditionalState(state);
    currentState = state;
    result = SymbolicValue.UNKNOWN_VALUE;
    scan(tree);
    return result;
  }

  @VisibleForTesting
  SymbolicValue evaluate(ExecutionState state, ConditionalState conditionalState, Tree tree) {
    currentConditionalState = conditionalState;
    currentState = state;
    result = SymbolicValue.UNKNOWN_VALUE;
    scan(tree);
    return result;
  }

  private SymbolicValue evaluate(Tree tree) {
    result = SymbolicValue.UNKNOWN_VALUE;
    scan(tree);
    return result;
  }

  @VisibleForTesting
  ConditionalState currentConditionalState;
  private ExecutionState currentState;
  private SymbolicValue result;

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    if (tree.is(Tree.Kind.CONDITIONAL_AND)) {
      evaluateConditionalAnd(tree);
    } else if (tree.is(Tree.Kind.CONDITIONAL_OR)) {
      evaluateConditionalOr(tree);
    } else if (tree.is(Tree.Kind.EQUAL_TO)) {
      evaluateRelationalOperator(tree, SymbolicRelation.EQUAL_TO);
    } else if (tree.is(Tree.Kind.GREATER_THAN)) {
      evaluateRelationalOperator(tree, SymbolicRelation.GREATER_THAN);
    } else if (tree.is(Tree.Kind.GREATER_THAN_OR_EQUAL_TO)) {
      evaluateRelationalOperator(tree, SymbolicRelation.GREATER_EQUAL);
    } else if (tree.is(Tree.Kind.LESS_THAN)) {
      evaluateRelationalOperator(tree, SymbolicRelation.LESS_THAN);
    } else if (tree.is(Tree.Kind.LESS_THAN_OR_EQUAL_TO)) {
      evaluateRelationalOperator(tree, SymbolicRelation.LESS_EQUAL);
    } else if (tree.is(Tree.Kind.NOT_EQUAL_TO)) {
      evaluateRelationalOperator(tree, SymbolicRelation.NOT_EQUAL);
    }
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    if ("false".equals(tree.value())) {
      result = SymbolicValue.BOOLEAN_FALSE;
    } else if ("true".equals(tree.value())) {
      result = SymbolicValue.BOOLEAN_TRUE;
    }
  }

  private void evaluateConditionalAnd(BinaryExpressionTree tree) {
    currentConditionalState = new ConditionalState(currentState);
    SymbolicValue leftResult = evaluate(tree.leftOperand());
    if (!leftResult.equals(SymbolicValue.BOOLEAN_FALSE)) {
      currentState = currentConditionalState.trueState;
      SymbolicValue rightResult = evaluate(tree.rightOperand());
      if (!rightResult.equals(SymbolicValue.BOOLEAN_FALSE)) {
        if (leftResult.equals(SymbolicValue.BOOLEAN_TRUE) && rightResult.equals(SymbolicValue.BOOLEAN_TRUE)) {
          result = SymbolicValue.BOOLEAN_TRUE;
        } else {
          result = SymbolicValue.UNKNOWN_VALUE;
        }
        return;
      }
    }
    result = SymbolicValue.BOOLEAN_FALSE;
  }

  private void evaluateConditionalOr(BinaryExpressionTree tree) {
    currentConditionalState = new ConditionalState(currentState);
    SymbolicValue leftResult = evaluate(tree.leftOperand());
    if (!leftResult.equals(SymbolicValue.BOOLEAN_TRUE)) {
      currentState = currentConditionalState.falseState;
      SymbolicValue rightResult = evaluate(tree.rightOperand());
      if (!rightResult.equals(SymbolicValue.BOOLEAN_TRUE)) {
        if (leftResult.equals(SymbolicValue.BOOLEAN_FALSE) && rightResult.equals(SymbolicValue.BOOLEAN_FALSE)) {
          result = SymbolicValue.BOOLEAN_FALSE;
        } else {
          result = SymbolicValue.UNKNOWN_VALUE;
        }
        return;
      }
    }
    result = SymbolicValue.BOOLEAN_TRUE;
  }

  private void evaluateRelationalOperator(BinaryExpressionTree tree, SymbolicRelation operator) {
    SymbolicValue leftValue = currentState.getSymbolicValue(tree.leftOperand());
    SymbolicValue rightValue = currentState.getSymbolicValue(tree.rightOperand());
    result = currentState.evaluateRelation(leftValue, operator, rightValue);
    if (result.equals(SymbolicValue.UNKNOWN_VALUE)) {
      currentConditionalState.trueState.setRelation(leftValue, operator, rightValue);
      currentConditionalState.falseState.setRelation(leftValue, operator.negate(), rightValue);
    }
  }

}
