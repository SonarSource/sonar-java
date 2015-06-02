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

import com.google.common.collect.ImmutableMap;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.symexec.SymbolicValue.SymbolicInstanceValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
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
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import javax.annotation.CheckForNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SymbolicEvaluator {

  private static final Map<Tree.Kind, SymbolicRelation> OPERATOR_TO_RELATION = ImmutableMap.<Tree.Kind, SymbolicRelation>builder()
    .put(Tree.Kind.EQUAL_TO, SymbolicRelation.EQUAL_TO)
    .put(Tree.Kind.GREATER_THAN, SymbolicRelation.GREATER_THAN)
    .put(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, SymbolicRelation.GREATER_EQUAL)
    .put(Tree.Kind.LESS_THAN, SymbolicRelation.LESS_THAN)
    .put(Tree.Kind.LESS_THAN_OR_EQUAL_TO, SymbolicRelation.LESS_EQUAL)
    .put(Tree.Kind.NOT_EQUAL_TO, SymbolicRelation.NOT_EQUAL)
    .build();

  public static void evaluateMethod(ExecutionState state, MethodTree tree, SymbolicExecutionCheck check) {
    if (tree.block() != null) {
      new SymbolicEvaluator(check).new SymbolicEvaluatorVisitor().evaluateWithState(state, tree.block());
    }
  }

  SymbolicEvaluator(SymbolicExecutionCheck check) {
    this.check = check;
  }

  private final SymbolicExecutionCheck check;

  private final AssignedSymbolExtractor extractor = new AssignedSymbolExtractor();

  SymbolicInstanceValue createSymbolicInstanceValue() {
    return new SymbolicInstanceValue();
  }

  private class SymbolicEvaluatorVisitor extends BaseTreeVisitor {
    private PackedStatementStates currentStates;

    private PackedStatementStates evaluateWithState(ExecutionState executionState, Tree tree) {
      currentStates = new PackedStatementStates();
      currentStates.addState(executionState);
      scan(tree);
      return currentStates;
    }

    /**
     * evaluates a condition.
     *
     * a condition is an expression (of type boolean), which can occur in the following places:
     * - condition of an if statement
     * - condition of a loop statement: do...while, for, while (but not foreach)
     * - left operand of a logical and && and or || operators
     * - first operand of a ternary operator
     * conditions of switch statement are broader and not evaluated using this method.
     *
     * this method evaluates the given condition. if the result is either true or false,
     * then the execution state is returned.
     * otherwise, if the result is unknown, then the execution state is split in two branches
     * with result TRUE and FALSE. whenever possible constraints are also set on the variables
     * found in the condition.
     *
     * @param state execution state
     * @param tree tree representing the condition
     * @return output states
     */
    private List<ExecutionState> evaluateCondition(ExecutionState executionState, ExpressionTree tree) {
      List<ExecutionState> result = new ArrayList<>();
      ExpressionTree expressionTree = removeCastAndParenthesis(tree);
      Tree.Kind kind = ((JavaTree) expressionTree).getKind();
      SymbolicRelation relation = OPERATOR_TO_RELATION.get(kind);
      if (relation != null) {
        evaluateConditionRelationalOperator(result, executionState, (BinaryExpressionTree) expressionTree, relation);
      } else {
        switch (kind) {
          case CONDITIONAL_AND:
            evaluateConditionConditionalAnd(result, executionState, (BinaryExpressionTree) expressionTree);
            break;
          case CONDITIONAL_OR:
            evaluateConditionConditionalOr(result, executionState, (BinaryExpressionTree) expressionTree);
            break;
          case IDENTIFIER:
            evaluateConditionIdentifier(result, executionState, ((IdentifierTree) expressionTree).symbol());
            break;
          default:
            evaluateConditionOther(result, executionState, expressionTree);
            break;
        }
      }
      return result;
    }

    private ExpressionTree removeCastAndParenthesis(ExpressionTree tree) {
      ExpressionTree result = tree;
      while (true) {
        if (result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
          result = ((ParenthesizedTree) result).expression();
        } else if (result.is(Tree.Kind.TYPE_CAST)) {
          result = ((TypeCastTree) result).expression();
        } else {
          return result;
        }
      }
    }

    private void evaluateConditionConditionalAnd(List<ExecutionState> result, ExecutionState executionState, BinaryExpressionTree tree) {
      for (ExecutionState conditionState : evaluateCondition(executionState, tree.leftOperand())) {
        if (conditionState.peek().equals(SymbolicValue.BOOLEAN_FALSE)) {
          result.add(conditionState);
        } else {
          conditionState.pop();
          result.addAll(evaluateCondition(conditionState, tree.rightOperand()));
        }
      }
    }

    private void evaluateConditionConditionalOr(List<ExecutionState> result, ExecutionState executionState, BinaryExpressionTree tree) {
      for (ExecutionState conditionState : evaluateCondition(executionState, tree.leftOperand())) {
        if (conditionState.peek().equals(SymbolicValue.BOOLEAN_TRUE)) {
          result.add(conditionState);
        } else {
          conditionState.pop();
          result.addAll(evaluateCondition(conditionState, tree.rightOperand()));
        }
      }
    }

    private void evaluateConditionIdentifier(List<ExecutionState> result, ExecutionState executionState, Symbol symbol) {
      SymbolicValue value = executionState.getValue(symbol);
      if (value == null) {
        value = createSymbolicInstanceValue();
        executionState.assignValue(symbol, value);
      }
      switch (executionState.getBooleanConstraint(value)) {
        case FALSE:
          result.add(executionState.push(SymbolicValue.BOOLEAN_FALSE));
          break;
        case TRUE:
          result.add(executionState.push(SymbolicValue.BOOLEAN_TRUE));
          break;
        default:
          result.add(new ExecutionState(executionState).setBooleanConstraint(value, SymbolicBooleanConstraint.FALSE).push(SymbolicValue.BOOLEAN_FALSE));
          result.add(new ExecutionState(executionState).setBooleanConstraint(value, SymbolicBooleanConstraint.TRUE).push(SymbolicValue.BOOLEAN_TRUE));
      }
    }

    private void evaluateConditionOther(List<ExecutionState> result, ExecutionState executionState, ExpressionTree expressionTree) {
      if (isSuperOrThisMemberSelect(expressionTree)) {
        evaluateConditionIdentifier(result, executionState, ((MemberSelectExpressionTree) expressionTree).identifier().symbol());
      } else {
        for (ExecutionState operatorState : evaluateWithState(executionState, expressionTree)) {
          if (operatorState.peek().equals(SymbolicValue.BOOLEAN_FALSE) || operatorState.peek().equals(SymbolicValue.BOOLEAN_TRUE)) {
            // condition is either true or false. nothing more needs to be done.
            result.add(operatorState);
          } else {
            // condition is unknown. split without setting constraints.
            operatorState.pop();
            result.add(new ExecutionState(operatorState).push(SymbolicValue.BOOLEAN_FALSE));
            result.add(new ExecutionState(operatorState).push(SymbolicValue.BOOLEAN_TRUE));
          }
        }
      }
    }

    private void evaluateConditionRelationalOperator(List<ExecutionState> result, ExecutionState executionState, BinaryExpressionTree tree, SymbolicRelation operator) {
      for (ExecutionState rightState : evaluateWithState(executionState, tree.leftOperand())) {
        for (ExecutionState operatorState : evaluateWithState(rightState, tree.rightOperand())) {
          SymbolicValue rightValue = operatorState.pop();
          SymbolicValue leftValue = operatorState.pop();
          SymbolicValue operatorResult = executionState.evaluateRelation(leftValue, operator, rightValue);
          if (SymbolicValue.BOOLEAN_FALSE.equals(operatorResult) || SymbolicValue.BOOLEAN_TRUE.equals(operatorResult)) {
            // condition is either true or false. nothing more needs to be done.
            result.add(executionState.push(operatorResult));
          } else {
            // condition is unknown. split with constraints.
            result.add(new ExecutionState(executionState).setRelation(leftValue, operator.negate(), rightValue).push(SymbolicValue.BOOLEAN_FALSE));
            result.add(new ExecutionState(executionState).setRelation(leftValue, operator, rightValue).push(SymbolicValue.BOOLEAN_TRUE));
          }
        }
      }
    }

    @Override
    public final void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
      super.visitArrayAccessExpression(tree);
      for (ExecutionState executionState : currentStates) {
        executionState.pop();
        executionState.pop();
        executionState.push(createSymbolicInstanceValue());
      }
    }

    @Override
    public final void visitAssignmentExpression(AssignmentExpressionTree tree) {
      super.visitAssignmentExpression(tree);
      for (ExecutionState executionState : currentStates) {
        SymbolicValue expression = executionState.pop();
        executionState.pop();
        Symbol.VariableSymbol symbol = extractVariableSymbol(tree.variable());
        if (symbol != null) {
          executionState.assignValue(symbol, expression);
        }
        executionState.push(expression);
      }
    }

    @Override
    public final void visitBinaryExpression(BinaryExpressionTree tree) {
      Tree.Kind kind = ((JavaTree) tree).getKind();
      SymbolicRelation relation = OPERATOR_TO_RELATION.get(kind);
      if (relation != null) {
        evaluateRelationalOperator(tree, relation);
      } else {
        switch (kind) {
          case CONDITIONAL_AND:
            evaluateConditionalAnd(tree);
            break;
          case CONDITIONAL_OR:
            evaluateConditionalOr(tree);
            break;
          default:
            evaluateBinaryExpressionOther(tree);
            break;
        }
      }
    }

    private void evaluateBinaryExpressionOther(BinaryExpressionTree tree) {
      super.visitBinaryExpression(tree);
      for (ExecutionState executionState : currentStates) {
        executionState.pop();
        executionState.pop();
        executionState.push(createSymbolicInstanceValue());
      }
    }

    private void evaluateConditionalAnd(BinaryExpressionTree tree) {
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      for (ExecutionState executionState : currentStates) {
        for (ExecutionState conditionState : evaluateCondition(executionState, tree.leftOperand())) {
          if (conditionState.getBooleanConstraint(conditionState.peek()).equals(SymbolicBooleanConstraint.FALSE)) {
            nextStates.addState(conditionState);
          } else {
            conditionState.pop();
            nextStates.addAll(evaluateWithState(conditionState, tree.rightOperand()));
          }
        }
      }
      currentStates = nextStates;
    }

    private void evaluateConditionalOr(BinaryExpressionTree tree) {
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      for (ExecutionState executionState : currentStates) {
        for (ExecutionState conditionState : evaluateCondition(executionState, tree.leftOperand())) {
          if (conditionState.getBooleanConstraint(conditionState.peek()).equals(SymbolicBooleanConstraint.TRUE)) {
            nextStates.addState(conditionState);
          } else {
            conditionState.pop();
            nextStates.addAll(evaluateWithState(conditionState, tree.rightOperand()));
          }
        }
      }
      currentStates = nextStates;
    }

    private void evaluateRelationalOperator(BinaryExpressionTree tree, SymbolicRelation operator) {
      scan(tree.leftOperand());
      scan(tree.rightOperand());
      for (ExecutionState executionState : currentStates) {
        SymbolicValue rightValue = executionState.pop();
        SymbolicValue leftValue = executionState.pop();
        SymbolicValue result = executionState.evaluateRelation(leftValue, operator, rightValue);
        executionState.push(result != null ? result : createSymbolicInstanceValue());
      }
    }

    @Override
    public final void visitConditionalExpression(ConditionalExpressionTree tree) {
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      for (ExecutionState executionState : currentStates) {
        for (ExecutionState conditionState : evaluateCondition(executionState, tree.condition())) {
          SymbolicValue result = conditionState.pop();
          if (conditionState.getBooleanConstraint(result).equals(SymbolicBooleanConstraint.FALSE)) {
            nextStates.addAll(evaluateWithState(conditionState, tree.falseExpression()));
          } else {
            nextStates.addAll(evaluateWithState(conditionState, tree.trueExpression()));
          }
        }
      }
      currentStates = nextStates;
    }

    @Override
    public final void visitIdentifier(IdentifierTree tree) {
      Symbol.VariableSymbol symbol = extractVariableSymbol(tree);
      if (symbol != null) {
        for (ExecutionState executionState : currentStates) {
          SymbolicValue value = executionState.getValue(symbol);
          if (value == null) {
            value = createSymbolicInstanceValue();
            executionState.assignValue(symbol, value);
          }
          executionState.push(value);
        }
      } else {
        SymbolicValue value = createSymbolicInstanceValue();
        for (ExecutionState executionState : currentStates) {
          executionState.push(value);
        }
      }
    }

    @Override
    public final void visitInstanceOf(InstanceOfTree tree) {
      SymbolicValue result = createSymbolicInstanceValue();
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      for (ExecutionState executionState : currentStates) {
        for (ExecutionState operandState : evaluateWithState(executionState, tree.expression())) {
          operandState.pop();
          nextStates.addState(operandState.push(result));
        }
      }
      currentStates = nextStates;
    }

    @Override
    public final void visitLiteral(LiteralTree tree) {
      SymbolicValue result;
      if (tree.is(Tree.Kind.INT_LITERAL, Tree.Kind.LONG_LITERAL)) {
        Long value = LiteralUtils.longLiteralValue(tree);
        result = value != null ? new SymbolicValue.SymbolicLongValue(value) : createSymbolicInstanceValue();
      } else if ("false".equals(tree.value())) {
        result = SymbolicValue.BOOLEAN_FALSE;
      } else if ("true".equals(tree.value())) {
        result = SymbolicValue.BOOLEAN_TRUE;
      } else {
        result = createSymbolicInstanceValue();
      }
      for (ExecutionState executionState : currentStates) {
        executionState.push(result);
      }
    }

    @Override
    public final void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      if (isSuperOrThisMemberSelect(tree)) {
        scan(tree.identifier());
      } else {
        scan(tree.expression());
        SymbolicValue value = createSymbolicInstanceValue();
        for (ExecutionState executionState : currentStates) {
          executionState.pop();
          executionState.push(value);
        }
      }
    }

    @Override
    public final void visitMethodInvocation(MethodInvocationTree tree) {
      scan(tree.methodSelect());
      scan(tree.arguments());
      for (ExecutionState executionState : currentStates) {
        SymbolicValue[] arguments = new SymbolicValue[tree.arguments().size()];
        for (int i = arguments.length - 1; i >= 0; i--) {
          arguments[i] = executionState.pop();
        }
        check.onExecutableElementInvocation(executionState, tree, Arrays.asList(arguments));
        executionState.invalidateFields(SymbolicEvaluator.this);
      }
    }

    @Override
    public final void visitNewArray(NewArrayTree tree) {
      scan(tree.dimensions());
      scan(tree.initializers());
      SymbolicValue value = createSymbolicInstanceValue();
      for (ExecutionState executionState : currentStates) {
        for (int i = tree.dimensions().size() + tree.initializers().size() - 1; i >= 0; i--) {
          executionState.pop();
        }
        executionState.push(value);
      }
    }

    @Override
    public final void visitNewClass(NewClassTree tree) {
      scan(tree.arguments());
      SymbolicValue newValue = createSymbolicInstanceValue();
      for (ExecutionState executionState : currentStates) {
        SymbolicValue[] arguments = new SymbolicValue[tree.arguments().size()];
        for (int i = arguments.length - 1; i >= 0; i--) {
          arguments[i] = executionState.pop();
        }
        executionState.push(newValue);
        check.onExecutableElementInvocation(executionState, tree, Arrays.asList(arguments));
      }
    }

    @Override
    public final void visitUnaryExpression(UnaryExpressionTree tree) {
      super.visitUnaryExpression(tree);
      if (tree.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
        evaluateLogicalNot();
      } else if (tree.is(Tree.Kind.POSTFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT, Tree.Kind.PREFIX_INCREMENT)) {
        evaluateUnaryWithSizeEffect(tree);
      }
    }

    private void evaluateLogicalNot() {
      for (ExecutionState executionState : currentStates) {
        SymbolicBooleanConstraint constraint = executionState.getBooleanConstraint(executionState.peek());
        if (constraint.equals(SymbolicBooleanConstraint.FALSE)) {
          executionState.pop();
          executionState.push(SymbolicValue.BOOLEAN_TRUE);
        } else if (constraint.equals(SymbolicBooleanConstraint.TRUE)) {
          executionState.pop();
          executionState.push(SymbolicValue.BOOLEAN_FALSE);
        }
      }
    }

    private void evaluateUnaryWithSizeEffect(UnaryExpressionTree tree) {
      for (ExecutionState executionState : currentStates) {
        Symbol.VariableSymbol symbol = extractVariableSymbol(tree.expression());
        if (symbol != null) {
          executionState.assignValue(symbol, createSymbolicInstanceValue());
        }
      }
    }

    @Override
    public void visitBreakStatement(BreakStatementTree tree) {
      currentStates.breakStates.addAll(currentStates.states);
      currentStates.states.clear();
    }

    @Override
    public void visitCaseGroup(CaseGroupTree tree) {
      scan(tree.body());
    }

    @Override
    public void visitContinueStatement(ContinueStatementTree tree) {
      currentStates = new PackedStatementStates(currentStates);
    }

    @Override
    public void visitDoWhileStatement(DoWhileStatementTree tree) {
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      Set<Symbol.VariableSymbol> assignedSymbols = extractor.findAssignedVariables(tree);
      invalidateAssignedVariables(assignedSymbols);
      scan(tree.statement());
      scan(tree.condition());
      for (ExecutionState executionState : currentStates) {
        if (!executionState.popLast().equals(SymbolicValue.BOOLEAN_TRUE)) {
          nextStates.addState(executionState);
        }
      }
      nextStates.addStates(currentStates.breakStates);
      currentStates = nextStates;
      invalidateAssignedVariables(assignedSymbols);
    }

    @Override
    public void visitExpressionStatement(ExpressionStatementTree tree) {
      scan(tree.expression());
      for (ExecutionState executionState : currentStates) {
        executionState.popLast();
      }
    }

    @Override
    public void visitForStatement(ForStatementTree tree) {
      scan(tree.initializer());
      if (tree.condition() != null) {
        evaluateForStatement(tree);
      } else {
        evaluateForStatementWithoutCondition(tree);
      }
    }

    private void evaluateForStatement(ForStatementTree tree) {
      // FIXME: variables in initializer should not be invalidated.
      Set<Symbol.VariableSymbol> assignedSymbols = extractor.findAssignedVariables(tree);
      invalidateAssignedVariables(assignedSymbols);
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      for (ExecutionState executionState : currentStates) {
        for (ExecutionState conditionState : evaluateCondition(executionState, tree.condition())) {
          SymbolicValue condition = conditionState.popLast();
          if (condition.equals(SymbolicValue.BOOLEAN_TRUE)) {
            currentStates = evaluateWithState(conditionState, tree.statement());
            scan(tree.update());
            nextStates.addStates(currentStates.breakStates);
          } else {
            nextStates.addState(conditionState);
          }
        }
      }
      currentStates = nextStates;
      invalidateAssignedVariables(assignedSymbols);
    }

    private void evaluateForStatementWithoutCondition(ForStatementTree tree) {
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      scan(tree.statement());
      scan(tree.update());
      nextStates.addStates(currentStates.breakStates);
      currentStates = nextStates;
    }

    @Override
    public void visitForEachStatement(ForEachStatement tree) {
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      invalidateAssignedVariables(extractor.findAssignedVariables(tree));
      for (ExecutionState executionState : currentStates) {
        for (ExecutionState expressionState : evaluateWithState(executionState, tree.expression())) {
          expressionState.popLast();
          nextStates.addState(new ExecutionState(expressionState));
          nextStates.addAll(evaluateWithState(new ExecutionState(expressionState), tree.statement()));
        }
      }
      currentStates = nextStates;
      invalidateAssignedVariables(extractor.findAssignedVariables(tree));
    }

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      for (ExecutionState executionState : currentStates) {
        for (ExecutionState conditionState : evaluateCondition(executionState, tree.condition())) {
          SymbolicValue condition = conditionState.popLast();
          check.onCondition(conditionState, tree, condition);
          if (conditionState.getBooleanConstraint(condition).equals(SymbolicBooleanConstraint.TRUE)) {
            nextStates.addAll(evaluateWithState(conditionState, tree.thenStatement()));
          } else if (tree.elseStatement() != null) {
            nextStates.addAll(evaluateWithState(conditionState, tree.elseStatement()));
          } else {
            nextStates.addState(conditionState);
          }
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
      scan(tree.expression());
      currentStates = new PackedStatementStates(currentStates);
    }

    @Override
    public void visitSwitchStatement(SwitchStatementTree tree) {
      boolean containsDefault = switchContainsDefault(tree);
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      for (ExecutionState executionState : currentStates) {
        for (ExecutionState expressionState : evaluateWithState(executionState, tree.expression())) {
          expressionState.popLast();
          for (int i = 0; i < tree.cases().size(); i++) {
            processCase(tree, i, new ExecutionState(expressionState));
            nextStates.addStates(currentStates.states);
            nextStates.addStates(currentStates.breakStates);
          }
          if (!containsDefault) {
            nextStates.addState(new ExecutionState(expressionState));
          }
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

    private void processCase(SwitchStatementTree tree, int caseIndex, ExecutionState state) {
      currentStates = new PackedStatementStates();
      currentStates.addState(state);
      for (int i = caseIndex; i < tree.cases().size(); i++) {
        scan(tree.cases().get(i));
      }
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
      scan(tree.expression());
      for (ExecutionState executionState : currentStates) {
        executionState.popLast();
      }
      scan(tree.block());
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      scan(tree.expression());
      for (ExecutionState executionState : currentStates) {
        executionState.popLast();
      }
      currentStates.states.clear();
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      scan(tree.block());
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      invalidateAssignedVariables(extractor.findAssignedVariables(tree));
      for (ExecutionState state : currentStates) {
        for (CatchTree catchTree : tree.catches()) {
          nextStates.addAll(evaluateWithState(new ExecutionState(state), catchTree.block()));
        }
        nextStates.addState(state);
      }
      currentStates = nextStates;
      scan(tree.finallyBlock());
    }

    @Override
    public void visitVariable(VariableTree tree) {
      if (tree.initializer() != null) {
        scan(tree.initializer());
        for (ExecutionState executionState : currentStates) {
          executionState.assignValue(tree.symbol(), executionState.popLast());
        }
      }
    }

    @Override
    public void visitWhileStatement(WhileStatementTree tree) {
      Set<Symbol.VariableSymbol> assignedSymbols = extractor.findAssignedVariables(tree);
      invalidateAssignedVariables(assignedSymbols);
      PackedStatementStates nextStates = new PackedStatementStates(currentStates);
      for (ExecutionState executionState : currentStates) {
        for (ExecutionState conditionState : evaluateCondition(executionState, tree.condition())) {
          SymbolicValue condition = conditionState.popLast();
          if (condition.equals(SymbolicValue.BOOLEAN_TRUE)) {
            nextStates.addStates(evaluateWithState(conditionState, tree.statement()).breakStates);
          } else {
            nextStates.addState(conditionState);
          }
        }
      }
      currentStates = nextStates;
      invalidateAssignedVariables(assignedSymbols);
    }

    void invalidateAssignedVariables(Set<Symbol.VariableSymbol> assignedVariables) {
      for (Symbol.VariableSymbol symbol : assignedVariables) {
        SymbolicValue value = createSymbolicInstanceValue();
        for (ExecutionState executionState : currentStates) {
          executionState.assignValue(symbol, value);
        }
      }
    }

    private final boolean isSuperOrThisMemberSelect(ExpressionTree tree) {
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
  }

  static class PackedStatementStates implements Iterable<ExecutionState> {
    final List<ExecutionState> breakStates = new ArrayList<>();
    final List<ExecutionState> states = new ArrayList<>();

    PackedStatementStates() {
    }

    PackedStatementStates(PackedStatementStates previousResult) {
      breakStates.addAll(previousResult.breakStates);
      previousResult.breakStates.clear();
    }

    public void addAll(PackedStatementStates packedStates) {
      breakStates.addAll(packedStates.breakStates);
      states.addAll(packedStates.states);
    }

    public void addState(ExecutionState state) {
      states.add(state);
    }

    public void addStates(List<ExecutionState> state) {
      states.addAll(state);
    }

    public boolean isEmpty() {
      return states.isEmpty();
    }

    @Override
    public Iterator<ExecutionState> iterator() {
      return states.iterator();
    }
  }

}
