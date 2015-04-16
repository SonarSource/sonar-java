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
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.CatchTree;
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
import java.util.Iterator;
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

  SymbolicBooleanConstraint evaluateExpression(ExecutionState state, ExpressionTree tree) {
    return new ExpressionVisitor().evaluate(state, tree);
  }

  PackedStatementStates evaluateStatement(ExecutionState state, StatementTree tree) {
    return new StatementVisitor().evaluate(PackedStatementStates.instantiateWithState(state), tree);
  }

  PackedStatementStates evaluateStatement(List<ExecutionState> states, StatementTree tree) {
    return new StatementVisitor().evaluate(PackedStatementStates.instantiateWithStates(states), tree);
  }

  PackedStatementStates evaluateStatement(PackedStatementStates states, Tree tree) {
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
    final SymbolicValue retrieveSymbolicValue(ExpressionTree tree) {
      ExpressionTree currentTree = tree;
      if (isSuperOrThisMemberSelect(tree)) {
        currentTree = ((MemberSelectExpressionTree) currentTree).identifier();
      }
      if (currentTree.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifierTree = (IdentifierTree) currentTree;
        Symbol symbol = identifierTree.symbol();
        if (symbol.isVariableSymbol()) {
          return new SymbolicValue.SymbolicVariableValue((Symbol.VariableSymbol) symbol);
        }
      } else {
        Long value = LiteralUtils.longLiteralValue(currentTree);
        if (value != null) {
          return new SymbolicValue.SymbolicLongValue(value);
        }
      }
      return null;
    }

    @CheckForNull
    final Symbol.VariableSymbol extractVariableSymbol(ExpressionTree tree) {
      Tree currentTree = tree;
      if (isSuperOrThisMemberSelect(tree)) {
        currentTree = ((MemberSelectExpressionTree) currentTree).identifier();
      }
      if (currentTree.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifierTree = (IdentifierTree) currentTree;
        Symbol symbol = identifierTree.symbol();
        if (symbol.isVariableSymbol()) {
          return (Symbol.VariableSymbol) symbol;
        }
      }
      return null;
    }

    final boolean isSuperOrThisMemberSelect(ExpressionTree tree) {
      if (tree.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelectTree = (MemberSelectExpressionTree) tree;
        if (memberSelectTree.expression().is(Tree.Kind.IDENTIFIER)) {
          IdentifierTree identifierExpression = (IdentifierTree) memberSelectTree.expression();
          if ("super".equals(identifierExpression.name()) || "this".equals(identifierExpression.name())) {
            return true;
          }
        }
      }
      return false;
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
      Symbol.VariableSymbol symbol = extractVariableSymbol(tree.variable());
      if (symbol != null) {
        currentState.setBooleanConstraint(new SymbolicValue.SymbolicVariableValue(symbol), SymbolicBooleanConstraint.UNKNOWN);
      }
      currentResult.unknownStates.add(currentState);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol.VariableSymbol symbol = extractVariableSymbol(tree);
      if (symbol != null) {
        SymbolicValue.SymbolicVariableValue value = new SymbolicValue.SymbolicVariableValue(symbol);
        switch (currentState.getBooleanConstraint(value)) {
          case FALSE:
            currentResult.falseStates.add(currentState);
            return;
          case TRUE:
            currentResult.trueStates.add(currentState);
            return;
          default:
            currentResult.falseStates.add(new ExecutionState(currentState).setBooleanConstraint(value, SymbolicBooleanConstraint.FALSE));
            currentResult.trueStates.add(new ExecutionState(currentState).setBooleanConstraint(value, SymbolicBooleanConstraint.TRUE));
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
      if (isSuperOrThisMemberSelect(tree)) {
        scan(tree.identifier());
      } else {
        currentResult.unknownStates.add(currentState);
      }
    }

    @Override
    public final void visitMethodInvocation(MethodInvocationTree tree) {
      currentState.invalidateFields();
      currentResult.unknownStates.add(currentState);
    }

    @Override
    public final void visitUnaryExpression(UnaryExpressionTree tree) {
      if (tree.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
        PackedStates unaryResult = evaluateCondition(currentState, tree.expression());
        currentResult.falseStates.addAll(unaryResult.trueStates);
        currentResult.trueStates.addAll(unaryResult.falseStates);
        currentResult.unknownStates.addAll(unaryResult.unknownStates);
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
      SymbolicValue leftValue = retrieveSymbolicValue(tree.leftOperand());
      SymbolicValue rightValue = retrieveSymbolicValue(tree.rightOperand());
      if (leftValue != null && rightValue != null) {
        switch (currentState.evaluateRelation(leftValue, operator, rightValue)) {
          case FALSE:
            currentResult.falseStates.add(currentState);
            break;
          case TRUE:
            currentResult.trueStates.add(currentState);
            break;
          default:
            currentResult.falseStates.add(new ExecutionState(currentState).setRelation(leftValue, operator.negate(), rightValue));
            currentResult.trueStates.add(new ExecutionState(currentState).setRelation(leftValue, operator, rightValue));
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
      Symbol.VariableSymbol symbol = extractVariableSymbol(tree.variable());
      if (symbol != null) {
        SymbolicValue.SymbolicVariableValue variable = new SymbolicValue.SymbolicVariableValue(symbol);
        currentState.invalidateRelationsOnValue(variable);
        currentState.setBooleanConstraint(variable, currentResult);
      }
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol.VariableSymbol symbol = extractVariableSymbol(tree);
      currentResult = symbol != null ? currentState.getBooleanConstraint(new SymbolicValue.SymbolicVariableValue(symbol)) : SymbolicBooleanConstraint.UNKNOWN;
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
      if (isSuperOrThisMemberSelect(tree)) {
        scan(tree.identifier());
      } else {
        super.visitMemberSelectExpression(tree);
        currentResult = SymbolicBooleanConstraint.UNKNOWN;
      }
    }

    @Override
    public final void visitMethodInvocation(MethodInvocationTree tree) {
      super.visitMethodInvocation(tree);
      currentState.invalidateFields();
      currentResult = SymbolicBooleanConstraint.UNKNOWN;
    }

    @Override
    public final void visitUnaryExpression(UnaryExpressionTree tree) {
      super.visitUnaryExpression(tree);
      if (tree.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
        currentResult = currentResult.negate();
      } else {
        if (tree.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT, Tree.Kind.PREFIX_INCREMENT)) {
          Symbol.VariableSymbol symbol = extractVariableSymbol(tree.expression());
          if (symbol != null) {
            currentState.invalidateRelationsOnValue(new SymbolicValue.SymbolicVariableValue(symbol));
          }
        }
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
      currentState.mergeRelations(Iterables.concat(leftStates.falseStates, leftStates.trueStates));
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
      currentState.mergeRelations(Iterables.concat(leftStates.falseStates, leftStates.trueStates));
    }

    @Override
    void evaluateRelationalOperator(BinaryExpressionTree tree, SymbolicRelation operator) {
      SymbolicValue leftValue = retrieveSymbolicValue(tree.leftOperand());
      SymbolicValue rightValue = retrieveSymbolicValue(tree.rightOperand());
      if (leftValue != null && rightValue != null) {
        currentResult = currentState.evaluateRelation(leftValue, operator, rightValue);
      } else {
        currentResult = SymbolicBooleanConstraint.UNKNOWN;
      }
    }
  }

  public class StatementVisitor extends BaseTreeVisitor {
    private PackedStatementStates currentStates;

    private PackedStatementStates evaluate(PackedStatementStates states, Tree tree) {
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
      currentStates = PackedStatementStates.instantiateWithBreakStates(currentStates.states);
    }

    @Override
    public void visitContinueStatement(ContinueStatementTree tree) {
      currentStates = PackedStatementStates.instantiate();
    }

    @Override
    public void visitDoWhileStatement(DoWhileStatementTree tree) {
      Set<Symbol.VariableSymbol> assignedSymbols = extractor.findAssignedVariables(tree);
      invalidateAssignedVariables(assignedSymbols);
      currentStates = evaluateStatement(currentStates, tree.statement());
      PackedStatementStates nextStates = PackedStatementStates.instantiate();
      for (ExecutionState state : currentStates) {
        if (evaluateExpression(state, tree.condition()) != SymbolicBooleanConstraint.TRUE) {
          nextStates.addState(state);
        }
      }
      currentStates = nextStates;
      invalidateAssignedVariables(assignedSymbols);
    }

    @Override
    public void visitExpressionStatement(ExpressionStatementTree tree) {
      for (ExecutionState state : currentStates) {
        evaluateExpression(state, tree.expression());
      }
    }

    @Override
    public void visitForStatement(ForStatementTree tree) {
      Set<Symbol.VariableSymbol> assignedSymbols = extractor.findAssignedVariables(tree);
      invalidateAssignedVariables(assignedSymbols);
      if (tree.condition() != null) {
        PackedStatementStates nextStates = PackedStatementStates.instantiate();
        for (ExecutionState state : currentStates) {
          PackedStates conditionStates = evaluateCondition(state, tree.condition());
          PackedStatementStates loopStates = evaluateStatement(conditionStates.trueStates, tree.statement());
          if (!conditionStates.falseStates.isEmpty() || !loopStates.isEmpty()) {
            state.mergeRelations(Iterables.concat(conditionStates.falseStates, loopStates));
            nextStates.addState(state);
          }
        }
        currentStates = nextStates;
        invalidateAssignedVariables(assignedSymbols);
      } else {
        evaluateStatement(currentStates, tree.statement());
        currentStates = PackedStatementStates.instantiate();
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
      PackedStatementStates nextStates = PackedStatementStates.instantiate();
      for (ExecutionState state : currentStates) {
        PackedStates conditionStates = evaluateCondition(state, tree.condition());
        result.put(tree, conditionStates.getBooleanConstraint().union(result.get(tree)));
        PackedStatementStates trueStates = evaluateStatement(conditionStates.trueStates, tree.thenStatement());
        PackedStatementStates falseStates;
        if (tree.elseStatement() == null) {
          falseStates = PackedStatementStates.instantiateWithStates(conditionStates.falseStates);
        } else {
          falseStates = evaluateStatement(conditionStates.falseStates, tree.elseStatement());
        }
        if (!falseStates.isEmpty() || !trueStates.isEmpty()) {
          state.mergeRelations(Iterables.concat(falseStates, trueStates));
          nextStates.addState(state);
        }
        nextStates.breakStates.addAll(falseStates.breakStates);
        nextStates.breakStates.addAll(trueStates.breakStates);
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
      currentStates = PackedStatementStates.instantiate();
    }

    @Override
    public void visitSwitchStatement(SwitchStatementTree tree) {
      PackedStatementStates nextStates = PackedStatementStates.instantiate();
      for (ExecutionState state : currentStates) {
        evaluateExpression(state, tree.expression());
        List<ExecutionState> endStates = new ArrayList<>();
        for (int i = 0; i < tree.cases().size(); i += 1) {
          PackedStatementStates caseStates = processCase(tree, i, new ExecutionState(state));
          endStates.addAll(caseStates.states);
          endStates.addAll(caseStates.breakStates);
        }
        if (!switchContainsDefault(tree)) {
          endStates.add(state);
        }
        if (!endStates.isEmpty()) {
          state.mergeRelations(endStates);
          nextStates.addState(state);
        }
      }
      currentStates = nextStates;
    }

    private boolean switchContainsDefault(SwitchStatementTree tree) {
      for (CaseGroupTree caseGroupTree : tree.cases()) {
        for (CaseLabelTree label : caseGroupTree.labels()) {
          if ("default".equals(label.caseOrDefaultKeyword().text())) {
            return true;
          }
        }
      }
      return false;
    }

    private PackedStatementStates processCase(SwitchStatementTree tree, int caseIndex, ExecutionState state) {
      PackedStatementStates caseStates = PackedStatementStates.instantiate();
      caseStates.addState(state);
      for (int i = caseIndex; i < tree.cases().size() && !caseStates.isEmpty(); i += 1) {
        caseStates = evaluateStatement(caseStates, tree.cases().get(i));
      }
      return caseStates;
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
      currentStates = PackedStatementStates.instantiate();
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      currentStates = evaluateStatement(currentStates, tree.block());
      invalidateAssignedVariables(extractor.findAssignedVariables(tree));
      for (ExecutionState state : currentStates) {
        List<ExecutionState> catchStates = new ArrayList<>();
        for (CatchTree catchTree : tree.catches()) {
          catchStates.addAll(evaluateStatement(new ExecutionState(state), catchTree.block()).states);
        }
        catchStates.add(state);
        state.mergeRelations(catchStates);
      }
      currentStates = evaluateStatement(currentStates, tree.finallyBlock());
    }

    @Override
    public void visitVariable(VariableTree tree) {
      if (tree.initializer() != null) {
        for (ExecutionState state : currentStates) {
          state.setBooleanConstraint(new SymbolicValue.SymbolicVariableValue((Symbol.VariableSymbol) tree.symbol()), evaluateExpression(state, tree.initializer()));
        }
      }
    }

    @Override
    public void visitWhileStatement(WhileStatementTree tree) {
      Set<Symbol.VariableSymbol> assignedSymbols = extractor.findAssignedVariables(tree);
      invalidateAssignedVariables(assignedSymbols);
      PackedStatementStates nextStates = PackedStatementStates.instantiate();
      for (ExecutionState state : currentStates) {
        PackedStates conditionStates = evaluateCondition(state, tree.condition());
        PackedStatementStates loopStates = evaluateStatement(conditionStates.trueStates, tree.statement());
        if (!conditionStates.falseStates.isEmpty() || !loopStates.isEmpty()) {
          state.mergeRelations(Iterables.concat(conditionStates.falseStates, loopStates));
          nextStates.addState(state);
        }
      }
      currentStates = nextStates;
      invalidateAssignedVariables(assignedSymbols);
    }

    void invalidateAssignedVariables(Set<Symbol.VariableSymbol> assignedVariables) {
      for (Symbol.VariableSymbol symbol : assignedVariables) {
        for (ExecutionState state : currentStates) {
          state.setBooleanConstraint(new SymbolicValue.SymbolicVariableValue(symbol), SymbolicBooleanConstraint.UNKNOWN);
        }
      }
    }
  }

  public static class PackedStates {
    final List<ExecutionState> falseStates = new ArrayList<>();
    final List<ExecutionState> trueStates = new ArrayList<>();
    final List<ExecutionState> unknownStates = new ArrayList<>();

    public void add(PackedStates that) {
      falseStates.addAll(that.falseStates);
      trueStates.addAll(that.trueStates);
      unknownStates.addAll(that.unknownStates);
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

  static class PackedStatementStates implements Iterable<ExecutionState> {
    final List<ExecutionState> breakStates = new ArrayList<>();
    final List<ExecutionState> states = new ArrayList<>();

    static PackedStatementStates instantiate() {
      return new PackedStatementStates();
    }

    static PackedStatementStates instantiateWithState(ExecutionState state) {
      return instantiateWithStates(ImmutableList.of(state));
    }

    static PackedStatementStates instantiateWithStates(List<ExecutionState> states) {
      PackedStatementStates result = new PackedStatementStates();
      result.states.addAll(states);
      return result;
    }

    static PackedStatementStates instantiateWithBreakStates(List<ExecutionState> breakStates) {
      PackedStatementStates result = new PackedStatementStates();
      result.breakStates.addAll(breakStates);
      return result;
    }

    public void addState(ExecutionState state) {
      states.add(state);
    }

    @Override
    public Iterator<ExecutionState> iterator() {
      return states.iterator();
    }

    public boolean isEmpty() {
      return states.isEmpty();
    }
  }

}
