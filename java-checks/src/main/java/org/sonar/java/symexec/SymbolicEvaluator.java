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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SymbolicEvaluator {

  private final AssignedSymbolExtractor extractor = new AssignedSymbolExtractor();

  private final Map<Tree, SymbolicBooleanConstraint> result = new HashMap<>();

  public Map<Tree, SymbolicBooleanConstraint> evaluateMethod(ExecutionState state, MethodTree tree) {
    result.clear();
    if (tree.block() != null) {
      evaluateStatement(ImmutableList.of(state), tree.block());
    }
    return result;
  }

  PackedStates evaluateCondition(ExecutionState state, ExpressionTree tree) {
    return new ConditionVisitor().evaluate(state, tree).splitUnknowns();
  }

  PackedStates evaluateCondition(List<ExecutionState> states, ExpressionTree tree) {
    PackedStates result = new PackedStates();
    for (ExecutionState state : states) {
      result.add(new ConditionVisitor().evaluate(state, tree).splitUnknowns());
    }
    return result;
  }

  SymbolicBooleanConstraint evaluateExpression(ExecutionState state, ExpressionTree tree) {
    return new ExpressionVisitor().evaluate(state, tree);
  }

  List<ExecutionState> evaluateStatement(List<ExecutionState> states, StatementTree tree) {
    return new StatementVisitor().evaluate(states, tree);
  }

  abstract class BaseExpressionVisitor extends BaseTreeVisitor {
    @Override
    public final void visitBinaryExpression(BinaryExpressionTree tree) {
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

    abstract void evaluateConditionalAnd(BinaryExpressionTree tree);

    abstract void evaluateConditionalOr(BinaryExpressionTree tree);

    abstract void evaluateRelationalOperator(BinaryExpressionTree tree, SymbolicRelation operator);

    @CheckForNull
    final Symbol.VariableSymbol extractLocalVariableSymbol(Tree tree) {
      if (tree.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifierTree = (IdentifierTree) tree;
        Symbol symbol = ((IdentifierTree) identifierTree).symbol();
        if (symbol.owner().isMethodSymbol() && symbol.isVariableSymbol()) {
          return (Symbol.VariableSymbol) symbol;
        }
      }
      return null;
    }
  }

  public class ConditionVisitor extends BaseExpressionVisitor {
    ExecutionState currentState;
    PackedStates currentResult;

    public PackedStates evaluate(ExecutionState state, ExpressionTree tree) {
      currentState = state;
      currentResult = new PackedStates();
      scan(tree);
      return currentResult;
    }

    @Override
    public final void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
      evaluateExpression(currentState, tree.expression());
      evaluateExpression(currentState, tree.index());
      currentResult.unknownStates.add(currentState);
    }

    @Override
    public final void visitAssignmentExpression(AssignmentExpressionTree tree) {
      evaluateExpression(currentState, tree.variable());
      evaluateExpression(currentState, tree.expression());
      PackedStates result = new PackedStates(currentState);
      result.setBooleanConstraintOnSymbol(extractLocalVariableSymbol(tree.variable()));
      currentResult.add(result);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol.VariableSymbol symbol = extractLocalVariableSymbol(tree);
      if (symbol != null) {
        switch (currentState.getBooleanConstraint(symbol)) {
          case FALSE:
            currentResult.falseStates.add(currentState);
            return;
          case TRUE:
            currentResult.trueStates.add(currentState);
            return;
          case UNKNOWN:
            currentResult.falseStates.add(new ExecutionState(currentState).setBooleanConstraint(symbol, SymbolicBooleanConstraint.FALSE));
            currentResult.trueStates.add(new ExecutionState(currentState).setBooleanConstraint(symbol, SymbolicBooleanConstraint.TRUE));
            return;
        }
      }
      currentResult.unknownStates.add(currentState);
    }

    @Override
    public final void visitInstanceOf(InstanceOfTree tree) {
      currentResult.unknownStates.add(currentState);
    }

    @Override
    public final void visitLiteral(LiteralTree tree) {
      if ("false".equals(tree.value())) {
        currentResult.falseStates.add(currentState);
      } else if ("true".equals(tree.value())) {
        currentResult.trueStates.add(currentState);
      } else {
        currentResult.unknownStates.add(currentState);
      }
    }

    @Override
    public final void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      currentResult.unknownStates.add(currentState);
    }

    @Override
    public final void visitMethodInvocation(MethodInvocationTree tree) {
      currentResult.unknownStates.add(currentState);
    }

    @Override
    public final void visitUnaryExpression(UnaryExpressionTree tree) {
      if (tree.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
        PackedStates result = evaluateCondition(currentState, tree.expression());
        currentResult.falseStates.addAll(result.trueStates);
        currentResult.trueStates.addAll(result.falseStates);
        currentResult.unknownStates.addAll(result.unknownStates);
      } else {
        currentResult.unknownStates.add(currentState);
      }
    }

    @Override
    void evaluateConditionalAnd(BinaryExpressionTree tree) {
      PackedStates leftResult = evaluateCondition(currentState, tree.leftOperand());
      currentResult.falseStates.addAll(leftResult.falseStates);
      for (ExecutionState state : leftResult.trueStates) {
        PackedStates rightResult = evaluateCondition(state, tree.rightOperand());
        currentResult.add(rightResult);
      }
    }

    @Override
    void evaluateConditionalOr(BinaryExpressionTree tree) {
      PackedStates leftResult = evaluateCondition(currentState, tree.leftOperand());
      currentResult.trueStates.addAll(leftResult.trueStates);
      for (ExecutionState state : leftResult.falseStates) {
        PackedStates rightResult = evaluateCondition(state, tree.rightOperand());
        currentResult.add(rightResult);
      }
    }

    @Override
    void evaluateRelationalOperator(BinaryExpressionTree tree, SymbolicRelation operator) {
      Symbol.VariableSymbol leftSymbol = extractLocalVariableSymbol(tree.leftOperand());
      Symbol.VariableSymbol rightSymbol = extractLocalVariableSymbol(tree.rightOperand());
      if (leftSymbol != null && rightSymbol != null) {
        switch (currentState.evaluateRelation(leftSymbol, operator, rightSymbol)) {
          case FALSE:
            currentResult.falseStates.add(currentState);
            break;
          case TRUE:
            currentResult.trueStates.add(currentState);
            break;
          case UNKNOWN:
            currentResult.falseStates.add(new ExecutionState(currentState).setRelation(leftSymbol, operator.negate(), rightSymbol));
            currentResult.trueStates.add(new ExecutionState(currentState).setRelation(leftSymbol, operator, rightSymbol));
        }
      } else {
        currentResult.unknownStates.add(currentState);
      }
    }
  }

  public class ExpressionVisitor extends BaseExpressionVisitor {
    ExecutionState currentState;
    SymbolicBooleanConstraint currentResult;

    public SymbolicBooleanConstraint evaluate(ExecutionState state, ExpressionTree tree) {
      currentState = state;
      currentResult = SymbolicBooleanConstraint.UNKNOWN;
      scan(tree);
      return currentResult;
    }

    @Override
    public final void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
      super.visitArrayAccessExpression(tree);
      currentResult = SymbolicBooleanConstraint.UNKNOWN;
    }

    @Override
    public final void visitAssignmentExpression(AssignmentExpressionTree tree) {
      super.visitAssignmentExpression(tree);
      Symbol.VariableSymbol symbol = extractLocalVariableSymbol(tree.variable());
      if (symbol != null) {
        currentState.setBooleanConstraint(symbol, currentResult);
      }
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol.VariableSymbol symbol = extractLocalVariableSymbol(tree);
      currentResult = symbol != null ? currentState.getBooleanConstraint(symbol) : SymbolicBooleanConstraint.UNKNOWN;
    }

    @Override
    public final void visitInstanceOf(InstanceOfTree tree) {
      currentResult = SymbolicBooleanConstraint.UNKNOWN;
    }

    @Override
    public final void visitLiteral(LiteralTree tree) {
      if ("false".equals(tree.value())) {
        currentResult = SymbolicBooleanConstraint.FALSE;
      } else if ("true".equals(tree.value())) {
        currentResult = SymbolicBooleanConstraint.TRUE;
      } else {
        currentResult = SymbolicBooleanConstraint.UNKNOWN;
      }
    }

    @Override
    public final void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      super.visitMemberSelectExpression(tree);
      currentResult = SymbolicBooleanConstraint.UNKNOWN;
    }

    @Override
    public final void visitMethodInvocation(MethodInvocationTree tree) {
      super.visitMethodInvocation(tree);
      currentResult = SymbolicBooleanConstraint.UNKNOWN;
    }

    @Override
    public final void visitUnaryExpression(UnaryExpressionTree tree) {
      super.visitUnaryExpression(tree);
      if (tree.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
        currentResult = currentResult.negate();
      } else {
        currentResult = SymbolicBooleanConstraint.UNKNOWN;
      }
    }

    @Override
    void evaluateConditionalAnd(BinaryExpressionTree tree) {
      PackedStates leftStates = evaluateCondition(currentState, tree.leftOperand());
      currentResult = leftStates.getBooleanConstraint();
      if (currentResult != SymbolicBooleanConstraint.FALSE) {
        currentResult = null;
        for (ExecutionState state : leftStates.trueStates) {
          currentResult = evaluateExpression(state, tree.rightOperand()).union(currentResult);
        }
        if (currentResult != SymbolicBooleanConstraint.FALSE) {
          currentResult = leftStates.getBooleanConstraint().union(currentResult);
        }
      }
      currentState.union(Iterables.concat(leftStates.falseStates, leftStates.trueStates));
    }

    @Override
    void evaluateConditionalOr(BinaryExpressionTree tree) {
      PackedStates leftStates = evaluateCondition(currentState, tree.leftOperand());
      currentResult = leftStates.getBooleanConstraint();
      if (currentResult != SymbolicBooleanConstraint.TRUE) {
        currentResult = null;
        for (ExecutionState state : leftStates.falseStates) {
          currentResult = evaluateExpression(state, tree.rightOperand()).union(currentResult);
        }
        if (currentResult != SymbolicBooleanConstraint.TRUE) {
          currentResult = leftStates.getBooleanConstraint().union(currentResult);
        }
      }
      currentState.union(Iterables.concat(leftStates.falseStates, leftStates.trueStates));
    }

    @Override
    void evaluateRelationalOperator(BinaryExpressionTree tree, SymbolicRelation operator) {
      Symbol.VariableSymbol leftSymbol = extractLocalVariableSymbol(tree.leftOperand());
      Symbol.VariableSymbol rightSymbol = extractLocalVariableSymbol(tree.rightOperand());
      if (leftSymbol != null && rightSymbol != null) {
        currentResult = currentState.evaluateRelation(leftSymbol, operator, rightSymbol);
      } else {
        currentResult = SymbolicBooleanConstraint.UNKNOWN;
      }
    }
  }

  public class StatementVisitor extends BaseTreeVisitor {
    private List<ExecutionState> currentStates;

    private List<ExecutionState> evaluate(List<ExecutionState> states, StatementTree tree) {
      currentStates = states;
      scan(tree);
      return currentStates;
    }

    @Override
    public void visitBlock(BlockTree tree) {
      for (StatementTree statement : tree.body()) {
        currentStates = evaluateStatement(currentStates, statement);
      }
    }

    @Override
    public void visitBreakStatement(BreakStatementTree tree) {
      currentStates = new ArrayList<>();
    }

    @Override
    public void visitContinueStatement(ContinueStatementTree tree) {
      currentStates = new ArrayList<>();
    }

    @Override
    public void visitDoWhileStatement(DoWhileStatementTree tree) {
      invalidateAssignedVariables(extractor.findAssignedVariables(tree));
      currentStates = evaluateStatement(currentStates, tree.statement());
      List<ExecutionState> newStates = new ArrayList<>();
      for (ExecutionState state : currentStates) {
        newStates.addAll(evaluateCondition(state, tree.condition()).falseStates);
      }
      currentStates = newStates;
      invalidateAssignedVariables(extractor.findAssignedVariables(tree));
    }

    @Override
    public void visitExpressionStatement(ExpressionStatementTree tree) {
      for (ExecutionState state : currentStates) {
        evaluateExpression(state, tree.expression());
      }
    }

    @Override
    public void visitForStatement(ForStatementTree tree) {
      invalidateAssignedVariables(extractor.findAssignedVariables(tree));
      if (tree.condition() != null) {
        PackedStates conditionStates = new PackedStates();
        for (ExecutionState state : currentStates) {
          conditionStates.add(evaluateCondition(state, tree.condition()));
        }
        currentStates = evaluateStatement(conditionStates.trueStates, tree.statement());
        currentStates = conditionStates.falseStates;
      } else {
        currentStates = evaluateStatement(currentStates, tree.statement());
        currentStates = new ArrayList<>();
      }
    }

    @Override
    public void visitForEachStatement(ForEachStatement tree) {
      for (ExecutionState state : currentStates) {
        evaluateExpression(state, tree.expression());
      }
      invalidateAssignedVariables(extractor.findAssignedVariables(tree));
      currentStates = evaluateStatement(currentStates, tree.statement());
      invalidateAssignedVariables(extractor.findAssignedVariables(tree));
    }

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      List<ExecutionState> nextStates = new ArrayList<>();
      for (ExecutionState state : currentStates) {
        PackedStates conditionStates = evaluateCondition(state, tree.condition());
        result.put(tree, conditionStates.getBooleanConstraint().union(result.get(tree)));
        List<ExecutionState> trueStates = evaluateStatement(conditionStates.trueStates, tree.thenStatement());
        List<ExecutionState> falseStates = conditionStates.falseStates;
        if (tree.elseStatement() != null) {
          falseStates = evaluateStatement(conditionStates.falseStates, tree.elseStatement());
        }
        if (!falseStates.isEmpty() || !trueStates.isEmpty()) {
          state.union(Iterables.concat(falseStates, trueStates));
          nextStates.add(state);
        }
      }
      currentStates = nextStates;
    }

    @Override
    public void visitLabeledStatement(LabeledStatementTree tree) {
      scan(tree.statement());
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      if (tree.expression() != null) {
        for (ExecutionState state : currentStates) {
          evaluateExpression(state, tree.expression());
        }
      }
      currentStates = new ArrayList<>();
    }

    @Override
    public void visitSwitchStatement(SwitchStatementTree tree) {
      for (ExecutionState state : currentStates) {
        evaluateExpression(state, tree.expression());
      }
      // TODO: stop evaluation for now
      currentStates = new ArrayList<>();
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
      for (ExecutionState state : currentStates) {
        evaluateExpression(state, tree.expression());
      }
      currentStates = evaluateStatement(currentStates, tree.block());
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      for (ExecutionState state : currentStates) {
        evaluateExpression(state, tree.expression());
      }
      currentStates = new ArrayList<>();
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      currentStates = evaluateStatement(currentStates, tree.block());
    }

    @Override
    public void visitVariable(VariableTree tree) {
      if (tree.initializer() != null) {
        for (ExecutionState state : currentStates) {
          state.setBooleanConstraint((Symbol.VariableSymbol) tree.symbol(), evaluateExpression(state, tree.initializer()));
        }
      }
    }

    @Override
    public void visitWhileStatement(WhileStatementTree tree) {
      invalidateAssignedVariables(extractor.findAssignedVariables(tree));
      PackedStates conditionStates = new PackedStates();
      for (ExecutionState state : currentStates) {
        conditionStates.add(evaluateCondition(state, tree.condition()));
      }
      evaluateStatement(conditionStates.trueStates, tree.statement());
      currentStates = conditionStates.falseStates;
      invalidateAssignedVariables(extractor.findAssignedVariables(tree));
    }

    void invalidateAssignedVariables(Set<Symbol.VariableSymbol> assignedVariables) {
      for (Symbol.VariableSymbol symbol : assignedVariables) {
        for (ExecutionState state : currentStates) {
          state.setBooleanConstraint(symbol, SymbolicBooleanConstraint.UNKNOWN);
        }
      }
    }
  }

  public static class PackedStates {
    public final List<ExecutionState> falseStates;
    public final List<ExecutionState> trueStates;
    public final List<ExecutionState> unknownStates;

    public PackedStates() {
      falseStates = new ArrayList<>();
      trueStates = new ArrayList<>();
      unknownStates = new ArrayList<>();
    }

    public PackedStates(ExecutionState unknownState) {
      falseStates = new ArrayList<>();
      trueStates = new ArrayList<>();
      this.unknownStates = new ArrayList<>();
      this.unknownStates.add(unknownState);
    }

    List<ExecutionState> toList() {
      List<ExecutionState> result = new ArrayList<>();
      result.addAll(falseStates);
      result.addAll(trueStates);
      result.addAll(unknownStates);
      return result;
    }

    public void add(PackedStates that) {
      falseStates.addAll(that.falseStates);
      trueStates.addAll(that.trueStates);
      unknownStates.addAll(that.unknownStates);
    }

    void setBooleanConstraintOnSymbol(Symbol.VariableSymbol symbol) {
      if (symbol != null) {
        for (ExecutionState state : falseStates) {
          state.setBooleanConstraint(symbol, SymbolicBooleanConstraint.FALSE);
        }
        for (ExecutionState state : trueStates) {
          state.setBooleanConstraint(symbol, SymbolicBooleanConstraint.TRUE);
        }
        for (ExecutionState state : unknownStates) {
          state.setBooleanConstraint(symbol, SymbolicBooleanConstraint.UNKNOWN);
        }
      }
    }

    PackedStates splitUnknowns() {
      for (ExecutionState state : unknownStates) {
        falseStates.add(new ExecutionState(state));
        trueStates.add(new ExecutionState(state));
      }
      unknownStates.clear();
      return this;
    }

    public boolean isAlwaysFalse() {
      return !falseStates.isEmpty() && trueStates.isEmpty() && unknownStates.isEmpty();
    }

    public boolean isAlwaysTrue() {
      return !trueStates.isEmpty() && falseStates.isEmpty() && unknownStates.isEmpty();
    }

    public boolean isUnknown() {
      return !unknownStates.isEmpty() || (!trueStates.isEmpty() && !falseStates.isEmpty());
    }

    public SymbolicBooleanConstraint getBooleanConstraint() {
      if (isAlwaysFalse()) {
        return SymbolicBooleanConstraint.FALSE;
      } else if (isAlwaysTrue()) {
        return SymbolicBooleanConstraint.TRUE;
      } else {
        return SymbolicBooleanConstraint.UNKNOWN;
      }
    }
  }

}
