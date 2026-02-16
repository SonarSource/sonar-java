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

import java.util.List;
import java.util.Set;

import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S8433")
public class FlexibleConstructorBodyValidationCheck extends FlexibleConstructorVisitor {

  private static final MethodMatchers VALIDATION_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.util.Objects")
      .names("requireNonNull", "requireNonNullElse", "requireNonNullElseGet", "checkIndex", "checkFromToIndex", "checkFromIndexSize")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("com.google.common.base.Preconditions")
      .names("checkNotNull", "checkArgument", "checkState")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("org.springframework.util.Assert")
      .names("notNull", "isTrue", "state", "hasLength", "hasText", "notEmpty")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("org.apache.commons.lang3.Validate")
      .names("notNull", "isTrue", "notEmpty", "notBlank")
      .withAnyParameters()
      .build()
  );

  @Override
  void validateConstructor(MethodTree constructor, List<StatementTree> body, int constructorCallIndex) {
    if (constructorCallIndex == body.size() - 1
      || (constructorCallIndex == -1)) {
      // No statements after constructor call or no constructor call
      return;
    }
    // Collect constructor parameters for analysis
    Set<Symbol> parameters = constructor.parameters().stream().map(VariableTree::symbol).collect(Collectors.toSet());

    // Analyze statements after the constructor call for movable validation
    for (int i = constructorCallIndex + 1; i < body.size(); i++) {
      StatementTree statement = body.get(i);

      if (isValidationStatement(statement) && canBeMovedToPrologue(statement, parameters)) {
        reportIssue(statement, "Move this validation logic before the super() or this() call.");
      }
    }
  }

  /**
   * Check if a statement is a validation statement (conditionally throws exception or calls validation method).
   *
   * @param statement the statement to check
   * @return true if the statement is a validation statement, false otherwise
   */
  private static boolean isValidationStatement(StatementTree statement) {
    if (statement instanceof IfStatementTree ifStatement) {
      return containsThrow(ifStatement.thenStatement());
    }

    if (statement instanceof ExpressionStatementTree expressionStatementTree
      && expressionStatementTree.expression() instanceof MethodInvocationTree methodInvocationTree) {
      return VALIDATION_METHODS.matches(methodInvocationTree);
    }

    return false;
  }

  /**
   * Check if a statement contains a throw statement in its body.
   */
  private static boolean containsThrow(Tree tree) {
    ThrowFinder finder = new ThrowFinder();
    tree.accept(finder);
    return finder.foundThrow;
  }

  /**
   * Check if validation logic can be safely moved to constructor prologue.
   * It can be moved if it only uses parameters, local variables, or static members.
   */
  private static boolean canBeMovedToPrologue(StatementTree statement, Set<Symbol> parameters) {
    InstanceDependencyCheck checker = new InstanceDependencyCheck(parameters);
    statement.accept(checker);
    return !checker.hasInstanceDependency;
  }

  /**
   * Visitor to find throw statements.
   */
  private static class ThrowFinder extends BaseTreeVisitor {
    boolean foundThrow = false;

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      foundThrow = true;
    }
  }

  /**
   * Visitor to check if code depends on instance members (fields or methods).
   */
  private static class InstanceDependencyCheck extends BaseTreeVisitor {
    private final Set<Symbol> parameters;
    boolean hasInstanceDependency = false;

    InstanceDependencyCheck(Set<Symbol> parameters) {
      this.parameters = parameters;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol symbol = tree.symbol();

      // Allow parameters, local variables and static fields / methods
      if (symbol.isLocalVariable() || symbol.isStatic() || parameters.contains(symbol)) {
        return;
      }

      // Check if it's an instance field or method being accessed
      if (symbol.isVariableSymbol()) {
        // This is likely an instance field accessed without 'this.'
        hasInstanceDependency = true;
        return;
      }

      super.visitIdentifier(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      ExpressionTree methodSelect = tree.methodSelect();

      // Check for implicit this (method call without any qualifier)
      if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
        Symbol.MethodSymbol methodSymbol = tree.methodSymbol();
        if (!methodSymbol.isStatic() && !methodSymbol.isUnknown()) {
          // This is an instance method call
          hasInstanceDependency = true;
          return;
        }
      }

      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      // skip throw statements
    }
  }
}
