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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.resolve.AnnotationInstance;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.java.resolve.Symbol.VariableSymbol;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Rule(
  key = "S2259",
  name = "Null pointers should not be dereferenced",
  tags = {"bug", "cert", "cwe", "owasp-a1", "owasp-a2", "owasp-a6", "security"},
  priority = Priority.BLOCKER)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("20min")
public class NullPointerCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String KEY = "S2259";
  private static final RuleKey RULE_KEY = RuleKey.of(CheckList.REPOSITORY_KEY, KEY);

  @Nullable
  private ConditionalState currentConditionalState;
  private JavaFileScannerContext context;
  private State currentState;
  private SemanticModel semanticModel;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    semanticModel = (SemanticModel) context.getSemanticModel();
    if (semanticModel != null) {
      context.getTree().accept(this);
    }
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    checkForIssue(tree.expression());
    super.visitArrayAccessExpression(tree);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    // currently we only handle assignment of the form: identifier =
    // TODO(merciesa): array[expr] = and mse.identifier = requires advanced tracking and are left outside of the scope
    if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
      VariableSymbol identifierSymbol = (VariableSymbol) semanticModel.getReference((IdentifierTreeImpl) tree.variable());
      AbstractValue value = tree.expression().is(Tree.Kind.NULL_LITERAL) ? AbstractValue.NULL : AbstractValue.UNKNOWN;
      currentState.setVariableValue(identifierSymbol, value);
    }
    super.visitAssignmentExpression(tree);
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    if (tree.is(Tree.Kind.CONDITIONAL_AND)) {
      visitorConditionalAnd(tree);
      return;
    }
    if (tree.is(Tree.Kind.CONDITIONAL_OR)) {
      visitConditionalOr(tree);
      return;
    }
    if (tree.is(Tree.Kind.EQUAL_TO)) {
      visitRelationalEqualTo(tree);
    } else if (tree.is(Tree.Kind.NOT_EQUAL_TO)) {
      visitRelationalNotEqualTo(tree);
    }
    super.visitBinaryExpression(tree);
  }

  @Override
  public void visitClass(ClassTree tree) {
    // state required for assignments in class body (e.g. static initializers, and int a, b, c = b = 0;)
    State oldState = currentState;
    currentState = new State();
    // skips modifiers, type parameters, super class and interfaces
    scan(tree.members());
    currentState = oldState;
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    // skips package name, imports, and annotations.
    scan(tree.types());
  }

  @Override
  public void visitConditionalExpression(ConditionalExpressionTree tree) {
    ConditionalState conditionalState = visitCondition(tree.condition());
    currentState = conditionalState.trueState;
    tree.trueExpression().accept(this);
    currentState = conditionalState.falseState;
    tree.falseExpression().accept(this);
    currentState = currentState.parentState.mergeValues(conditionalState.trueState, conditionalState.falseState);
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    currentState.invalidateValuesOfHierarchy();
    currentState = new State(currentState);
    scan(tree.statement());
    visitCondition(tree.condition());
    restorePreviousState();
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    scan(tree.initializer());
    ConditionalState conditionalState = visitCondition(tree.condition());
    currentState.invalidateValuesOfHierarchy();
    currentState = conditionalState.trueState;
    scan(tree.statement());
    scan(tree.update());
    restorePreviousState();
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    scan(tree.expression());
    currentState.invalidateValuesOfHierarchy();
    currentState = new State(currentState);
    scan(tree.statement());
    restorePreviousState();
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    ConditionalState conditionalState = visitCondition(tree.condition());
    currentState = conditionalState.trueState;
    tree.thenStatement().accept(this);
    if (tree.elseStatement() != null) {
      currentState = conditionalState.falseState;
      tree.elseStatement().accept(this);
    }
    currentState = currentState.parentState.mergeValues(conditionalState.trueState, conditionalState.falseState);
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    checkForIssue(tree.expression());
    super.visitMemberSelectExpression(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    State oldState = currentState;
    currentState = new State();
    scan(tree.block());
    currentState = oldState;
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    Symbol symbol = tree.symbol();
    if (symbol.isMethodSymbol()) {
      MethodSymbol methodSymbol = (MethodSymbol) symbol;
      List<org.sonar.java.resolve.Symbol> parameters = methodSymbol.getParameters().scopeSymbols();
      if (parameters.size() != 0) {
        for (int i = 0; i < tree.arguments().size(); i += 1) {
          // in case of varargs, there could be more arguments than parameters. in that case, pick the last parameter.
          if (checkNullity(parameters.get(i < parameters.size() ? i : parameters.size() - 1)) == AbstractValue.NOTNULL) {
            this.checkForIssue(tree.arguments().get(i));
          }
        }
      }
    }
    super.visitMethodInvocation(tree);
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    scan(tree.resources());
    State blockState = new State(currentState);
    currentState = blockState;
    scan(tree.block());
    for (CatchTree catchTree : tree.catches()) {
      currentState = new State(blockState.parentState);
      scan(catchTree);
      blockState.mergeValues(currentState, null);
    }
    if (tree.finallyBlock() != null) {
      currentState = new State(blockState.parentState);
      scan(tree.finallyBlock());
      blockState.mergeValues(currentState, null);
    }
    currentState = blockState.parentState.mergeValues(blockState, null);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    // skips modifiers (annotations) and type.
    if (tree.initializer() != null) {
      currentState.setVariableValue((VariableSymbol) tree.symbol(), checkNullity(tree.initializer()));
      scan(tree.initializer());
    }
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    ConditionalState conditionalState = visitCondition(tree.condition());
    currentState.invalidateValuesOfHierarchy();
    currentState = conditionalState.trueState;
    scan(tree.statement());
    restorePreviousState();
  }

  private AbstractValue checkNullity(Symbol symbol) {
    for (AnnotationInstance annotation : ((org.sonar.java.resolve.Symbol) symbol).metadata().annotations()) {
      if (annotation.isTyped("javax.annotation.Nonnull")) {
        return AbstractValue.NOTNULL;
      }
      if (annotation.isTyped("javax.annotation.CheckForNull") || annotation.isTyped("javax.annotation.Nullable")) {
        return AbstractValue.NULL;
      }
    }
    // FIXME(merciesa): should use annotation on package and class
    return AbstractValue.UNKNOWN;
  }

  public AbstractValue checkNullity(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = semanticModel.getReference((IdentifierTreeImpl) tree);
      if (symbol != null && symbol.isVariableSymbol()) {
        AbstractValue value = currentState.getVariableValue((VariableSymbol) symbol);
        if (value != AbstractValue.UNKNOWN) {
          return value;
        }
        return checkNullity(symbol);
      }
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      Symbol symbol = ((MethodInvocationTree) tree).symbol();
      if (symbol.isMethodSymbol()) {
        return checkNullity(symbol);
      }
    } else if (tree.is(Tree.Kind.NULL_LITERAL)) {
      return AbstractValue.NULL;
    }
    // FIXME(merciesa): should use annotation on package and class
    return AbstractValue.UNKNOWN;
  }

  // raises an issue if the passed tree can be null.
  private void checkForIssue(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = semanticModel.getReference((IdentifierTreeImpl) tree);
      if (isSymbolLocalVariableOrMethodParameter(symbol) && isVariableNull((VariableSymbol) symbol)) {
        context.addIssue(tree, RULE_KEY, String.format("%s can be null.", symbol.name()));
      }
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      Symbol symbol = ((MethodInvocationTree) tree).symbol();
      if (symbol.isMethodSymbol() && checkNullity(symbol) == AbstractValue.NULL) {
        context.addIssue(tree, RULE_KEY, String.format("Value returned by method '%s' can be null.", symbol.name()));
      }
    } else if (tree.is(Tree.Kind.NULL_LITERAL)) {
      context.addIssue(tree, RULE_KEY, "null is dereferenced or passed as argument.");
    }
  }

  private boolean isSymbolLocalVariableOrMethodParameter(Symbol symbol) {
    return symbol != null && symbol.isVariableSymbol() && symbol.owner().isMethodSymbol();
  }

  private boolean isVariableNull(VariableSymbol symbol) {
    return currentState.getVariableValue((VariableSymbol) symbol) == AbstractValue.NULL;
  }

  private void restorePreviousState() {
    currentState = currentState.parentState;
  }

  public enum AbstractValue {
    // value is known to be not null.
    NOTNULL,
    // value is known to be null.
    NULL,
    // value is unknown (could be null or not null).
    UNKNOWN
  }

  private ConditionalState visitCondition(ExpressionTree tree) {
    ConditionalState oldConditionalState = currentConditionalState;
    ConditionalState conditionalState = new ConditionalState(currentState);
    currentConditionalState = conditionalState;
    scan(tree);
    currentConditionalState = oldConditionalState;
    return conditionalState;
  }

  private ConditionalState visitCondition(ExpressionTree tree, State newState) {
    State oldState = currentState;
    currentState = newState;
    ConditionalState result = visitCondition(tree);
    currentState = oldState;
    return result;
  }

  private void visitorConditionalAnd(BinaryExpressionTree tree) {
    ConditionalState leftConditionalState = visitCondition(tree.leftOperand());
    // in case of a conditional and, the current state for the right operand is the true state of the left one,
    // because the right operand is evaluated only if the left operand was true.
    ConditionalState rightConditionalState = visitCondition(tree.rightOperand(), leftConditionalState.trueState);
    if (currentConditionalState != null) {
      currentConditionalState.mergeConditionalAnd(leftConditionalState, rightConditionalState);
    }
  }

  private void visitConditionalOr(BinaryExpressionTree tree) {
    ConditionalState leftConditionalState = visitCondition(tree.leftOperand());
    // in case of a conditional or, the current state for the right operand is the false state of the left one,
    // because of the right operand is evaluated only if the left operand was false.
    ConditionalState rightConditionalState = visitCondition(tree.rightOperand(), leftConditionalState.falseState);
    if (currentConditionalState != null) {
      currentConditionalState.mergeConditionalOr(leftConditionalState, rightConditionalState);
    }
  }

  // extracts the symbol in case of ident <op> null, or null <op> ident.
  @Nullable
  private VariableSymbol extractRelationalSymbol(BinaryExpressionTree tree) {
    if (tree.leftOperand().is(Tree.Kind.NULL_LITERAL) && tree.rightOperand().is(Tree.Kind.IDENTIFIER)) {
      return (VariableSymbol) semanticModel.getReference((IdentifierTreeImpl) tree.rightOperand());
    } else if (tree.leftOperand().is(Tree.Kind.IDENTIFIER) && tree.rightOperand().is(Tree.Kind.NULL_LITERAL)) {
      return (VariableSymbol) semanticModel.getReference((IdentifierTreeImpl) tree.leftOperand());
    } else {
      return null;
    }
  }

  private void visitRelationalEqualTo(BinaryExpressionTree tree) {
    VariableSymbol symbol = extractRelationalSymbol(tree);
    if (symbol != null && currentConditionalState != null) {
      currentConditionalState.trueState.setVariableValue(symbol, AbstractValue.NULL);
      currentConditionalState.falseState.setVariableValue(symbol, AbstractValue.NOTNULL);
    }
  }

  private void visitRelationalNotEqualTo(BinaryExpressionTree tree) {
    VariableSymbol symbol = extractRelationalSymbol(tree);
    if (symbol != null && currentConditionalState != null) {
      currentConditionalState.trueState.setVariableValue(symbol, AbstractValue.NOTNULL);
      currentConditionalState.falseState.setVariableValue(symbol, AbstractValue.NULL);
    }
  }

  @VisibleForTesting
  static class ConditionalState {
    final State falseState;
    final State trueState;

    ConditionalState(State currentState) {
      falseState = new State(currentState);
      trueState = new State(currentState);
    }

    void mergeConditionalAnd(ConditionalState leftConditionalState, ConditionalState rightConditionalState) {
      // copies the learned values to the parent.
      trueState.copyValuesFrom(leftConditionalState.trueState);
      trueState.copyValuesFrom(rightConditionalState.trueState);
      // invalidates both false states and copies values to the parent
      falseState.copyValuesFrom(leftConditionalState.falseState.invalidateValues());
      falseState.copyValuesFrom(rightConditionalState.falseState.invalidateValues());
    }

    void mergeConditionalOr(ConditionalState leftConditionalState, ConditionalState rightConditionalState) {
      // invalidates both true states and copies value to the parent.
      trueState.copyValuesFrom(leftConditionalState.trueState.invalidateValues());
      trueState.copyValuesFrom(rightConditionalState.trueState.invalidateValues());
      // copies the learned values to the parent.
      falseState.copyValuesFrom(leftConditionalState.falseState);
      falseState.copyValuesFrom(rightConditionalState.falseState);
    }
  }

  @VisibleForTesting
  static class State {
    @Nullable
    final State parentState;
    final Map<VariableSymbol, AbstractValue> variables;

    public State() {
      this.parentState = null;
      this.variables = new HashMap<>();
    }

    public State(State parentState) {
      this.parentState = parentState;
      this.variables = new HashMap<>();
    }

    // returns the value of the variable in the current state.
    public AbstractValue getVariableValue(VariableSymbol variable) {
      for (State state = this; state != null; state = state.parentState) {
        AbstractValue result = state.variables.get(variable);
        if (result != null) {
          return result;
        }
      }
      return AbstractValue.UNKNOWN;
    }

    // sets the value of the variable in the current state.
    public void setVariableValue(VariableSymbol variable, AbstractValue value) {
      variables.put(variable, value);
    }

    /**
     * copies the value of each variables in fromState to this
     *
     * @param fromState state from which the values must be copied.
     */
    public void copyValuesFrom(State fromState) {
      for (VariableSymbol variable : fromState.variables.keySet()) {
        this.setVariableValue(variable, fromState.getVariableValue(variable));
      }
    }

    /**
     * sets all the variables registered in this state and all the previous states to UNKNOWN.
     *
     * @return this
     */
    public State invalidateValuesOfHierarchy() {
      for (State state = this; state != null; state = state.parentState)
        for (VariableSymbol variable : state.variables.keySet()) {
          setVariableValue(variable, AbstractValue.UNKNOWN);
        }
      return this;
    }

    /**
     * sets all the variables registered in this state to UNKNOWN.
     *
     * @return this
     */
    public State invalidateValues() {
      for (VariableSymbol variable : variables.keySet()) {
        setVariableValue(variable, AbstractValue.UNKNOWN);
      }
      return this;
    }

    /**
     * merges the values of the variables from state1 and state2 into this.
     *
     * the set of all variables in state1 union state2 is first built,
     * then the variables in this set are queried in both states (their values fall back to the parent state if they are not found).
     * their value are then set in the parent state (either to the corresponding value if they are equal, or to UNKNOWN).
     *
     * @param state1 first state to merge
     * @param state2 second state to merge or null
     * @return this
     */
    public State mergeValues(State state1, @Nullable State state2) {
      Set<VariableSymbol> variables = new HashSet<>();
      variables.addAll(state1.variables.keySet());
      if (state2 != null) {
        variables.addAll(state2.variables.keySet());
      }
      for (VariableSymbol variable : variables) {
        AbstractValue currentValue = getVariableValue(variable);
        AbstractValue trueValue = state1.variables.get(variable);
        if (trueValue == null) {
          trueValue = currentValue;
        }
        AbstractValue falseValue = state2 != null ? state2.variables.get(variable) : currentValue;
        if (falseValue == null) {
          falseValue = currentValue;
        }
        // both null -> null; both notnull -> notnull; else unknown
        setVariableValue(variable, trueValue == falseValue ? trueValue : AbstractValue.UNKNOWN);
      }
      return this;
    }
  }

}
