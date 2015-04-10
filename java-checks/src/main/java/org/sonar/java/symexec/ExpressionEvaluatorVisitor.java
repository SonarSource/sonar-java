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
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.List;

public class ExpressionEvaluatorVisitor extends BaseTreeVisitor {

  public ExpressionEvaluatorVisitor(ExecutionState state, Tree tree) {
    currentState = state;
    scan(tree);
  }

  private final ExecutionState currentState;
  @VisibleForTesting
  final List<ExecutionState> falseStates = new ArrayList<>();
  @VisibleForTesting
  final List<ExecutionState> trueStates = new ArrayList<>();

  public boolean isAlwaysFalse() {
    return !falseStates.isEmpty() && trueStates.isEmpty();
  }

  public boolean isAwlaysTrue() {
    return !trueStates.isEmpty() && falseStates.isEmpty();
  }

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
  public void visitIdentifier(IdentifierTree tree) {
    Symbol.VariableSymbol symbol = extractLocalVariableSymbol(tree);
    if (symbol != null) {
      SymbolicBooleanConstraint constraint = currentState.getBooleanConstraint(symbol);
      switch (constraint) {
        case FALSE:
          falseStates.add(currentState);
          break;
        case TRUE:
          trueStates.add(currentState);
          break;
        case UNKNOWN:
          falseStates.add(new ExecutionState(currentState).setBooleanConstraint(symbol, SymbolicBooleanConstraint.FALSE));
          trueStates.add(new ExecutionState(currentState).setBooleanConstraint(symbol, SymbolicBooleanConstraint.TRUE));
          break;
        default:
          throw new IllegalStateException("illegal value " + constraint);
      }
    } else {
      falseStates.add(currentState);
      trueStates.add(currentState);
    }
  }

  @Override
  public void visitInstanceOf(InstanceOfTree tree) {
    falseStates.add(currentState);
    trueStates.add(currentState);
  }

  @Override
  public void visitLiteral(LiteralTree tree) {
    if ("false".equals(tree.value())) {
      falseStates.add(currentState);
    } else if ("true".equals(tree.value())) {
      trueStates.add(currentState);
    } else {
      falseStates.add(currentState);
      trueStates.add(currentState);
    }
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    falseStates.add(currentState);
    trueStates.add(currentState);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    falseStates.add(currentState);
    trueStates.add(currentState);
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    if (tree.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
      ExpressionEvaluatorVisitor currentResults = new ExpressionEvaluatorVisitor(currentState, tree.expression());
      falseStates.addAll(currentResults.trueStates);
      trueStates.addAll(currentResults.falseStates);
    } else {
      falseStates.add(currentState);
      trueStates.add(currentState);
    }
  }

  private void evaluateConditionalAnd(BinaryExpressionTree tree) {
    ExpressionEvaluatorVisitor leftResults = new ExpressionEvaluatorVisitor(currentState, tree.leftOperand());
    falseStates.addAll(leftResults.falseStates);
    evaluateConditionalOperator(tree, leftResults.trueStates);
  }

  private void evaluateConditionalOr(BinaryExpressionTree tree) {
    ExpressionEvaluatorVisitor leftResults = new ExpressionEvaluatorVisitor(currentState, tree.leftOperand());
    trueStates.addAll(leftResults.trueStates);
    evaluateConditionalOperator(tree, leftResults.falseStates);
  }

  private void evaluateConditionalOperator(BinaryExpressionTree tree, List<ExecutionState> longpathStates) {
    for (ExecutionState state : longpathStates) {
      ExpressionEvaluatorVisitor rightResults = new ExpressionEvaluatorVisitor(state, tree.rightOperand());
      falseStates.addAll(rightResults.falseStates);
      trueStates.addAll(rightResults.trueStates);
    }
  }

  private void evaluateRelationalOperator(BinaryExpressionTree tree, SymbolicRelation operator) {
    Symbol.VariableSymbol leftSymbol = extractLocalVariableSymbol(tree.leftOperand());
    Symbol.VariableSymbol rightSymbol = extractLocalVariableSymbol(tree.rightOperand());
    if (leftSymbol != null && rightSymbol != null) {
      SymbolicBooleanConstraint constraint = currentState.evaluateRelation(leftSymbol, operator, rightSymbol);
      switch (constraint) {
        case FALSE:
          falseStates.add(currentState);
          break;
        case TRUE:
          trueStates.add(currentState);
          break;
        case UNKNOWN:
          falseStates.add(new ExecutionState(currentState).setRelation(leftSymbol, operator.negate(), rightSymbol));
          trueStates.add(new ExecutionState(currentState).setRelation(leftSymbol, operator, rightSymbol));
          break;
        default:
          throw new IllegalStateException("illegal value " + constraint);
      }
    } else {
      falseStates.add(currentState);
      trueStates.add(currentState);
    }
  }

  @CheckForNull
  private Symbol.VariableSymbol extractLocalVariableSymbol(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) tree;
      Symbol symbol = identifierTree.symbol();
      if (symbol.owner().isMethodSymbol()) {
        return (Symbol.VariableSymbol) symbol;
      }
    }
    return null;
  }

}
