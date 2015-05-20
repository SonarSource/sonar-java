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
import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaSymbol.MethodJavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.symexecengine.State;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.VariableSymbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
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
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("10min")
public class NullPointerCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String MESSAGE_NULLABLE_EXPRESSION = "NullPointerException might be thrown as '%s' is nullable here";

  private static final String MESSAGE_NULL_LITERAL = "null is dereferenced";

  @Nullable
  private ConditionalState currentConditionalState;
  private JavaFileScannerContext context;
  private ExecutionState currentState;
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
    checkForIssue(tree.expression(), MESSAGE_NULLABLE_EXPRESSION, MESSAGE_NULL_LITERAL);
    super.visitArrayAccessExpression(tree);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);
    if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
      Symbol identifierSymbol = ((IdentifierTree) tree.variable()).symbol();
      if (identifierSymbol.isVariableSymbol()) {
        currentState.setVariableState(identifierSymbol, checkNullity(tree.expression()));
      }
    }
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
    ExecutionState oldState = currentState;
    currentState = new ExecutionState();
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
    currentState = currentState.parent.overrideBy(conditionalState.trueState.merge(conditionalState.falseState));
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    invalidateVariables(currentState, new AssignmentVisitor().findAssignedVariables(tree.statement()));
    currentState = new ExecutionState(currentState);
    scan(tree.statement());
    scan(tree.condition());
    restorePreviousState();
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    scan(tree.initializer());
    ConditionalState conditionalState = visitCondition(tree.condition());
    Set<VariableSymbol> assignedVariables = new AssignmentVisitor().findAssignedVariables(tree.statement());
    assignedVariables.addAll(new AssignmentVisitor().findAssignedVariables(tree.update()));
    currentState = conditionalState.trueState;
    invalidateVariables(currentState, assignedVariables);
    scan(tree.statement());
    scan(tree.update());
    restorePreviousState();
    invalidateVariables(currentState, assignedVariables);
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    scan(tree.expression());
    invalidateVariables(currentState, new AssignmentVisitor().findAssignedVariables(tree.statement()));
    currentState = new ExecutionState(currentState);
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
    currentState = currentState.parent.overrideBy(conditionalState.trueState.merge(conditionalState.falseState));
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    checkForIssue(tree.expression(), MESSAGE_NULLABLE_EXPRESSION, MESSAGE_NULL_LITERAL);
    super.visitMemberSelectExpression(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    ExecutionState oldState = currentState;
    currentState = new ExecutionState();
    for (VariableTree parameter : tree.parameters()) {
      Symbol symbol = parameter.symbol();
      currentState.setVariableState(symbol, checkNullity(symbol));
    }
    scan(tree.block());
    currentState = oldState;
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    Symbol symbol = tree.symbol();
    if (symbol.isMethodSymbol()) {
      MethodJavaSymbol methodSymbol = (MethodJavaSymbol) symbol;
      List<JavaSymbol> parameters = methodSymbol.getParameters().scopeSymbols();
      if (!parameters.isEmpty()) {
        for (int i = 0; i < tree.arguments().size(); i += 1) {
          // in case of varargs, there could be more arguments than parameters. in that case, pick the last parameter.
          if (checkNullity(parameters.get(i < parameters.size() ? i : parameters.size() - 1)).equals(NullableState.NOTNULL)) {
            this.checkForIssue(tree.arguments().get(i),
              String.format("'%%s' is nullable here and method '%s' does not accept nullable argument", methodSymbol.name()),
              String.format("method '%s' does not accept nullable argument", methodSymbol.name()));
          }
        }
      }
    }
    super.visitMethodInvocation(tree);
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    checkForIssue(tree.expression(), MESSAGE_NULLABLE_EXPRESSION, MESSAGE_NULL_LITERAL);
    scan(tree.expression());
    Set<VariableSymbol> variables = new AssignmentVisitor().findAssignedVariables(tree.cases());
    invalidateVariables(currentState, variables);
    for (CaseGroupTree caseTree : tree.cases()) {
      currentState = new ExecutionState(currentState);
      scan(caseTree);
      restorePreviousState();
    }
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    scan(tree.resources());
    ExecutionState blockState = new ExecutionState(currentState);
    currentState = blockState;
    scan(tree.block());
    for (CatchTree catchTree : tree.catches()) {
      currentState = new ExecutionState(blockState.parent);
      scan(catchTree);
      blockState.merge(currentState);
    }
    if (tree.finallyBlock() != null) {
      currentState = new ExecutionState(blockState.parent);
      scan(tree.finallyBlock());
      blockState.merge(currentState);
    }
    currentState = blockState.parent.merge(blockState);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    // skips modifiers (annotations) and type.
    if (tree.initializer() != null) {
      scan(tree.initializer());
      currentState.setVariableState(tree.symbol(), checkNullity(tree.initializer()));
    }
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    ConditionalState conditionalState = visitCondition(tree.condition());
    Set<VariableSymbol> assignedVariables = new AssignmentVisitor().findAssignedVariables(tree.statement());
    currentState = conditionalState.trueState;
    invalidateVariables(currentState, assignedVariables);
    scan(tree.statement());
    restorePreviousState();
    invalidateVariables(currentState, assignedVariables);
  }

  private NullableState checkNullity(Symbol symbol) {
    if (symbol.metadata().isAnnotatedWith("javax.annotation.Nonnull")) {
      return NullableState.NOTNULL;
    } else if (symbol.metadata().isAnnotatedWith("javax.annotation.CheckForNull")) {
      return NullableState.NULL;
    }
    return NullableState.UNKNOWN;
  }

  public NullableState checkNullity(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      return checkNullity((IdentifierTree) tree);
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      Symbol symbol = ((MethodInvocationTree) tree).symbol();
      if (symbol.isMethodSymbol()) {
        return checkNullity(symbol);
      }
    } else if (tree.is(Tree.Kind.NULL_LITERAL)) {
      return NullableState.NULL;
    }
    return NullableState.UNKNOWN;
  }

  public NullableState checkNullity(IdentifierTree tree) {
    Symbol symbol = tree.symbol();
    if (isSymbolLocalVariableOrMethodParameter(symbol)) {
      return currentState.getVariableState((VariableSymbol) symbol);
    }
    return NullableState.UNKNOWN;
  }

  /**
   * raises an issue if the passed tree can be null.
   *
   * @param tree tree that must be checked
   * @param nullableMessage message to generate when the tree is an identifier or a method invocation
   * @param nullMessage message to generate when the tree is the null literal
   */
  private void checkForIssue(Tree tree, String nullableMessage, String nullMessage) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) tree).symbol();
      if (checkNullity(tree).equals(NullableState.NULL)) {
        // prevents reporting issue multiple times
        currentState.setVariableState(symbol, NullableState.UNKNOWN);
        context.addIssue(tree, this, String.format(nullableMessage, symbol.name()));
      }
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      Symbol symbol = ((MethodInvocationTree) tree).symbol();
      if (checkNullity(symbol).equals(NullableState.NULL)) {
        context.addIssue(tree, this, String.format(nullableMessage, symbol.name()));
      }
    } else if (tree.is(Tree.Kind.NULL_LITERAL)) {
      context.addIssue(tree, this, nullMessage);
    }
  }

  private boolean isSymbolLocalVariableOrMethodParameter(Symbol symbol) {
    return symbol.isVariableSymbol() && symbol.owner().isMethodSymbol();
  }

  private void restorePreviousState() {
    currentState = currentState.parent;
  }

  private ExecutionState invalidateVariables(ExecutionState executionState, Set<VariableSymbol> variables) {
    for (VariableSymbol variable : variables) {
      executionState.setVariableState(variable, NullableState.UNKNOWN);
    }
    return executionState;
  }

  private ConditionalState visitCondition(ExpressionTree tree) {
    ConditionalState oldConditionalState = currentConditionalState;
    ConditionalState conditionalState = new ConditionalState(currentState);
    currentConditionalState = conditionalState;
    scan(tree);
    currentConditionalState = oldConditionalState;
    return conditionalState;
  }

  private ConditionalState visitCondition(ExpressionTree tree, ExecutionState newState) {
    ExecutionState oldState = currentState;
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
    Symbol symbol = null;
    if (tree.leftOperand().is(Tree.Kind.NULL_LITERAL) && tree.rightOperand().is(Tree.Kind.IDENTIFIER)) {
      symbol = ((IdentifierTree) tree.rightOperand()).symbol();
    } else if (tree.leftOperand().is(Tree.Kind.IDENTIFIER) && tree.rightOperand().is(Tree.Kind.NULL_LITERAL)) {
      symbol = ((IdentifierTree) tree.leftOperand()).symbol();
    }
    if (symbol == null || !symbol.isVariableSymbol()) {
      return null;
    }
    return (VariableSymbol) symbol;
  }

  private void visitRelationalEqualTo(BinaryExpressionTree tree) {
    VariableSymbol symbol = extractRelationalSymbol(tree);
    if (symbol != null && currentConditionalState != null) {
      currentConditionalState.trueState.setVariableState(symbol, NullableState.NULL);
      currentConditionalState.falseState.setVariableState(symbol, NullableState.NOTNULL);
    }
  }

  private void visitRelationalNotEqualTo(BinaryExpressionTree tree) {
    VariableSymbol symbol = extractRelationalSymbol(tree);
    if (symbol != null && currentConditionalState != null) {
      currentConditionalState.trueState.setVariableState(symbol, NullableState.NOTNULL);
      currentConditionalState.falseState.setVariableState(symbol, NullableState.NULL);
    }
  }

  @VisibleForTesting
  static class AssignmentVisitor extends BaseTreeVisitor {
    @VisibleForTesting
    Set<VariableSymbol> assignedSymbols = new HashSet<>();

    public Set<VariableSymbol> findAssignedVariables(Tree tree) {
      tree.accept(this);
      return assignedSymbols;
    }

    public Set<VariableSymbol> findAssignedVariables(List<? extends Tree> trees) {
      for (Tree tree : trees) {
        tree.accept(this);
      }
      return assignedSymbols;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
        registerAssignedSymbol(((IdentifierTree) tree.variable()).symbol());
      }
      super.visitAssignmentExpression(tree);
    }

    @VisibleForTesting
    void registerAssignedSymbol(Symbol symbol) {
      if (symbol.isVariableSymbol()) {
        assignedSymbols.add((VariableSymbol) symbol);
      }
    }
  }

  public static class NullableState extends State {
    // value is known to be not null.
    public static final NullableState NOTNULL = new NullableState();
    // value is known to be null.
    public static final NullableState NULL = new NullableState();
    // value is unknown (could be null or not null).
    public static final NullableState UNKNOWN = new NullableState();

    @Override
    public State merge(State s) {
      return this.equals(s) ? this : UNKNOWN;
    }
  }

  @VisibleForTesting
  static class ConditionalState {
    final ExecutionState falseState;
    final ExecutionState trueState;

    ConditionalState(ExecutionState currentState) {
      falseState = new ExecutionState(currentState);
      trueState = new ExecutionState(currentState);
    }

    void mergeConditionalAnd(ConditionalState leftConditionalState, ConditionalState rightConditionalState) {
      trueState.overrideBy(leftConditionalState.trueState.overrideBy(rightConditionalState.trueState));
      falseState.merge(leftConditionalState.falseState.merge(rightConditionalState.falseState));
    }

    void mergeConditionalOr(ConditionalState leftConditionalState, ConditionalState rightConditionalState) {
      trueState.merge(leftConditionalState.trueState.merge(rightConditionalState.trueState));
      falseState.overrideBy(leftConditionalState.falseState.overrideBy(rightConditionalState.falseState));
    }
  }

  @VisibleForTesting
  static class ExecutionState {
    @Nullable
    final ExecutionState parent;
    final Map<Symbol, NullableState> variables;

    public ExecutionState() {
      this.parent = null;
      this.variables = new HashMap<>();
    }

    public ExecutionState(ExecutionState parentState) {
      this.parent = parentState;
      this.variables = new HashMap<>();
    }

    // returns the value of the variable in the current state.
    public NullableState getVariableState(Symbol variable) {
      for (ExecutionState currentState = this; currentState != null; currentState = currentState.parent) {
        NullableState result = currentState.variables.get(variable);
        if (result != null) {
          return result;
        }
      }
      return NullableState.UNKNOWN;
    }

    // sets the value of the variable in the current state.
    public void setVariableState(Symbol variable, NullableState state) {
      variables.put(variable, state);
    }

    ExecutionState merge(ExecutionState executionState) {
      for (Symbol variable : Iterables.concat(variables.keySet(), executionState.variables.keySet())) {
        NullableState state = executionState.getVariableState(variable);
        NullableState currentState = getVariableState(variable);
        setVariableState(variable, (NullableState) state.merge(currentState));
      }
      return this;
    }

    ExecutionState overrideBy(ExecutionState executionState) {
      for (Map.Entry<Symbol, NullableState> entry : executionState.variables.entrySet()) {
        variables.put(entry.getKey(), entry.getValue());
      }
      return this;
    }
  }

}
