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
package org.sonar.java.symexec;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;

public class ExpressionEvaluatorVisitor extends BaseTreeVisitor {

  public SymbolicBooleanConstraint evaluate(ExecutionState state, Tree tree) {
    currentConditionalState = new ConditionalState(state);
    currentState = state;
    result = SymbolicBooleanConstraint.UNKNOWN;
    scan(tree);
    return result;
  }

  @VisibleForTesting
  SymbolicBooleanConstraint evaluate(ExecutionState state, ConditionalState conditionalState, Tree tree) {
    currentConditionalState = conditionalState;
    currentState = state;
    result = SymbolicBooleanConstraint.UNKNOWN;
    scan(tree);
    return result;
  }

  private SymbolicBooleanConstraint evaluate(Tree tree) {
    result = SymbolicBooleanConstraint.UNKNOWN;
    scan(tree);
    return result;
  }

  @VisibleForTesting
  ConditionalState currentConditionalState;
  private ExecutionState currentState;
  private SymbolicBooleanConstraint result;

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
      result = SymbolicBooleanConstraint.FALSE;
    } else if ("true".equals(tree.value())) {
      result = SymbolicBooleanConstraint.TRUE;
    }
  }

  private void evaluateConditionalAnd(BinaryExpressionTree tree) {
    currentConditionalState = new ConditionalState(currentState);
    SymbolicBooleanConstraint leftResult = evaluate(tree.leftOperand());
    if (leftResult != SymbolicBooleanConstraint.FALSE) {
      currentState = currentConditionalState.trueState;
      SymbolicBooleanConstraint rightResult = evaluate(tree.rightOperand());
      if (rightResult != SymbolicBooleanConstraint.FALSE) {
        if (leftResult == SymbolicBooleanConstraint.TRUE && rightResult == SymbolicBooleanConstraint.TRUE) {
          result = SymbolicBooleanConstraint.TRUE;
        } else {
          result = SymbolicBooleanConstraint.UNKNOWN;
        }
        return;
      }
    }
    result = SymbolicBooleanConstraint.FALSE;
  }

  private void evaluateConditionalOr(BinaryExpressionTree tree) {
    currentConditionalState = new ConditionalState(currentState);
    SymbolicBooleanConstraint leftResult = evaluate(tree.leftOperand());
    if (leftResult != SymbolicBooleanConstraint.TRUE) {
      currentState = currentConditionalState.falseState;
      SymbolicBooleanConstraint rightResult = evaluate(tree.rightOperand());
      if (rightResult != SymbolicBooleanConstraint.TRUE) {
        if (leftResult == SymbolicBooleanConstraint.FALSE && rightResult == SymbolicBooleanConstraint.FALSE) {
          result = SymbolicBooleanConstraint.FALSE;
        } else {
          result = SymbolicBooleanConstraint.UNKNOWN;
        }
        return;
      }
    }
    result = SymbolicBooleanConstraint.TRUE;
  }

  private void evaluateRelationalOperator(BinaryExpressionTree tree, SymbolicRelation operator) {
    Symbol.VariableSymbol leftSymbol = extractLocalVariableSymbol(tree.leftOperand());
    Symbol.VariableSymbol rightSymbol = extractLocalVariableSymbol(tree.rightOperand());
    if (leftSymbol != null && rightSymbol != null) {
      result = currentState.evaluateRelation(leftSymbol, operator, rightSymbol);
      if (result == SymbolicBooleanConstraint.UNKNOWN) {
        currentConditionalState.trueState.setRelation(leftSymbol, operator, rightSymbol);
        currentConditionalState.falseState.setRelation(leftSymbol, operator.negate(), rightSymbol);
      }
    }
  }

  @CheckForNull
  private Symbol.VariableSymbol extractLocalVariableSymbol(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) tree;
      Symbol symbol = ((IdentifierTree) identifierTree).symbol();
      if (symbol.owner().isMethodSymbol()) {
        return (Symbol.VariableSymbol) symbol;
      }
    }
    return null;
  }

}
