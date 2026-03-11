/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6207")
public class RedundantRecordMethodsCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree targetRecord = (ClassTree) tree;

    List<Symbol.VariableSymbol> components = targetRecord.recordComponents().stream()
      .map(VariableTree::symbol)
      .filter(Symbol.VariableSymbol.class::isInstance)
      .map(Symbol.VariableSymbol.class::cast)
      .toList();
    Set<String> componentNames = components.stream()
      .map(Symbol.VariableSymbol::name)
      .collect(Collectors.toSet());

    for (Tree member : targetRecord.members()) {
      if (member.is(Tree.Kind.CONSTRUCTOR)) {
        checkConstructor((MethodTree) member, componentNames);
      } else if (member.is(Tree.Kind.METHOD)) {
        checkMethod((MethodTree) member, components);
      }
    }
  }

  private void checkConstructor(MethodTree constructor, Set<String> componentNames) {
    if (isAnnotated(constructor)) {
      return;
    }
    if (constructor.parameters().isEmpty() && constructor.block().body().isEmpty()) {
      reportIssue(constructor.simpleName(), "Remove this useless empty compact constructor.");
      return;
    }
    if (constructor.parameters().size() != componentNames.size()
      || constructor.parameters().stream().anyMatch(parameter -> !componentNames.contains(parameter.symbol().name()))) {
      return;
    }

    ConstructorExecutionState executionState = new ConstructorExecutionState(componentNames);
    constructor.block().body().forEach(executionState::applyStatement);

    if (!executionState.componentsAreFullyAssigned() || executionState.logicAfterAssignments) {
      return;
    }
    if (executionState.logicBeforeAssignments) {
      reportIssue(constructor.simpleName(), "Consider using a compact constructor here.");
    } else {
      reportIssue(constructor.simpleName(), "Remove this redundant constructor which is the same as a default one.");
    }
  }

  private void checkMethod(MethodTree method, List<Symbol.VariableSymbol> components) {
    if (isAnnotated(method) || !method.parameters().isEmpty()) {
      return;
    }

    String methodName = method.symbol().name();
    Symbol accessedComponent = components.stream().filter(c -> c.name().equals(methodName)).findFirst().orElse(null);
    if (accessedComponent == null) {
      return;
    }

    var accessorVisitor = new AccessorVisitor();
    method.block().accept(accessorVisitor);
    if (accessorVisitor.containsLogic) {
      return;
    }
    if (accessorVisitor.returnedExpressions.stream().allMatch(e -> isComponent(e, accessedComponent))) {
      reportIssue(method.simpleName(), "Remove this redundant method which is the same as a default one.");
    }
  }

  private static boolean isComponent(ExpressionTree expression, Symbol component) {
    return (ExpressionUtils.skipParentheses(expression) instanceof IdentifierTree identifier && component.equals(identifier.symbol()))
      || (expression instanceof MemberSelectExpressionTree memberSelect && component.equals(memberSelect.identifier().symbol()));
  }

  private static boolean isAnnotated(MethodTree method) {
    return !method.modifiers().annotations().isEmpty();
  }

  /**
   * Class to perform a simple symbolic execution of a record constructor. The state keeps track of which components have been assigned to the corresponding parameters and which
   * parameters have been changed from their initial values, as well as if the constructor contains logic before or after assignments to components.
   */
  private static class ConstructorExecutionState {
    /**
     * Immutable set of the names of components in the record.
     */
    private final Set<String> componentNames;

    /**
     * Set of names of components of the record that have been assigned to their corresponding parameter so far.
     */
    private final Set<String> assignedComponents = new HashSet<>();

    /**
     * Set of names of parameters that still hold the value of the argument passed to the constructor.
     */
    private final Set<String> unchangedParameters = new HashSet<>();

    /**
     * Flag indicating whether the constructor has logic before any of the component assignments.
     */
    boolean logicBeforeAssignments = false;

    /**
     * Flag indicating whether the constructor has logic after any of the component assignments.
     */
    boolean logicAfterAssignments = false;

    private ConstructorExecutionState(Set<String> componentNames) {
      this.componentNames = componentNames;
      unchangedParameters.addAll(componentNames);
    }

    public boolean componentsAreFullyAssigned() {
      return assignedComponents.size() == componentNames.size();
    }

    public void applyStatement(StatementTree statement) {
      if (statement instanceof ExpressionStatementTree expression
        && expression.expression() instanceof AssignmentExpressionTree assignment) {
        applyAssignment(assignment.variable(), assignment.expression());
      } else if (statement instanceof IfStatementTree ifStatement) {
        applyIf(ifStatement);
      } else if (statement instanceof BlockTree block) {
        block.body().forEach(this::applyStatement);
      } else if (this.assignedComponents.isEmpty()) {
        applyLogic();
      }
    }

    private void applyAssignment(ExpressionTree lhs, ExpressionTree rhs) {
      if (rhs instanceof IdentifierTree identifier
        && isParameter(identifier.symbol())
        && unchangedParameters.contains(identifier.name())) {
        Optional<String> component = getComponent(lhs).filter(name -> name.equals(identifier.name()));
        if (component.isPresent()) {
          assignedComponents.add(component.get());
        } else {
          applyLogic();
        }
      } else if (lhs instanceof IdentifierTree identifier && isParameter(identifier.symbol())) {
        unchangedParameters.remove(identifier.name());
      } else {
        applyLogic();
      }
    }

    private void applyIf(IfStatementTree ifStatement) {
      ConstructorExecutionState stateCopy = copy();
      applyStatement(ifStatement.thenStatement());
      StatementTree elseStatement = ifStatement.elseStatement();
      if (elseStatement != null) {
        stateCopy.applyStatement(elseStatement);
      }
      mergeWith(stateCopy);
    }

    private void applyLogic() {
      if (this.assignedComponents.isEmpty()) {
        logicBeforeAssignments = true;
      } else {
        logicAfterAssignments = true;
      }
    }

    /**
     * Return the name of the component if the given expression is of the form `this.name`.
     */
    private Optional<String> getComponent(ExpressionTree expression) {
      if (expression instanceof MemberSelectExpressionTree memberSelect) {
        String name = memberSelect.identifier().name();
        if (componentNames.contains(name)) {
          return Optional.of(name);
        }
      }
      return Optional.empty();
    }

    private boolean isParameter(Symbol symbol) {
      return componentNames.contains(symbol.name());
    }

    private ConstructorExecutionState copy() {
      var copy = new ConstructorExecutionState(componentNames);
      copy.unchangedParameters.clear();
      copy.unchangedParameters.addAll(unchangedParameters);
      copy.assignedComponents.addAll(assignedComponents);
      copy.logicBeforeAssignments = logicBeforeAssignments;
      copy.logicAfterAssignments = logicAfterAssignments;
      return copy;
    }

    /**
     * When joining two branches of the execution, a component is considered assigned if it was assigned in both branches.
     * A parameter is considered unchanged if it was unchanged on both branches.
     * Logic before or after assignments is considered present if either branch has them.
     */
    private void mergeWith(ConstructorExecutionState other) {
      assignedComponents.removeIf(component -> !other.assignedComponents.contains(component));
      unchangedParameters.removeIf(component -> !other.unchangedParameters.contains(component));
      logicBeforeAssignments = logicBeforeAssignments || other.logicBeforeAssignments;
      logicAfterAssignments = logicAfterAssignments || other.logicAfterAssignments;
    }
  }

  private static class AccessorVisitor extends BaseTreeVisitor {

    Set<ExpressionTree> returnedExpressions = new HashSet<>();

    boolean containsLogic = false;

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      returnedExpressions.add(tree.expression());
      super.visitReturnStatement(tree);
    }

    @Override
    public void visitAssertStatement(AssertStatementTree tree) {
      containsLogic = true;
      super.visitAssertStatement(tree);
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      containsLogic = true;
      super.visitThrowStatement(tree);
    }

    @Override
    public void visitExpressionStatement(ExpressionStatementTree tree) {
      containsLogic = true;
      super.visitExpressionStatement(tree);
    }

    @Override
    public void visitVariable(VariableTree tree) {
      containsLogic = true;
      super.visitVariable(tree);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      containsLogic = true;
      super.visitAssignmentExpression(tree);
    }
  }
}
