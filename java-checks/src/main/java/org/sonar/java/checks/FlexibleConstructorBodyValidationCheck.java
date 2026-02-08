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
import java.util.Set;

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S8433")
public class FlexibleConstructorBodyValidationCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

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
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava25Compatible();
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree constructor = (MethodTree) tree;
    BlockTree body = constructor.block();

    if (body == null || body.body().isEmpty()) {
      return;
    }

    // Find the super() or this() call
    int constructorCallIndex = findConstructorCallIndex(body);

    // Get statements after the constructor call
    List<StatementTree> statements = body.body();
    if (constructorCallIndex == statements.size() - 1) {
      // No statements after constructor call
      return;
    }

    // Collect constructor parameters for analysis
    Set<Symbol> parameters = new HashSet<>();
    constructor.parameters().forEach(param -> parameters.add(param.symbol()));

    // Analyze statements after the constructor call for movable validation
    for (int i = constructorCallIndex + 1; i < statements.size(); i++) {
      StatementTree statement = statements.get(i);

      if (isValidationStatement(statement) && canBeMovedToPrologue(statement, parameters)) {
        reportIssue(statement, "Move this validation logic before the super() or this() call.");
      } else if (containsFieldAssignment(statement)) {
        // Stop analysis if we encounter a field assignment (indicates validation should not be moved)
        break;
      }
    }
  }

  /**
   * Find the index of an explicit super() / this() call in the constructor body.
   * Returns -1 if no explicit call is found (implicit super()).
   */
  private static int findConstructorCallIndex(BlockTree body) {
    List<StatementTree> statements = body.body();
    for (int i = 0; i < statements.size(); i++) {
      StatementTree statement = statements.get(i);
      if (!statement.is(Tree.Kind.EXPRESSION_STATEMENT)
        || !(((ExpressionStatementTree) statement).expression()).is(Tree.Kind.METHOD_INVOCATION)) {
        continue;
      }
      MethodInvocationTree invocation = (MethodInvocationTree) ((ExpressionStatementTree) statement).expression();
      ExpressionTree methodSelect = invocation.methodSelect();
      if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
        String methodName = ((IdentifierTree) methodSelect).name();
        if (ExpressionUtils.isThisOrSuper(methodName)) {
          return i;
        }
      }
    }
    // No explicit super() or this() call
    return -1;
  }

  /**
   * Check if a statement is a validation statement (throws exception or calls validation method).
   */
  private static boolean isValidationStatement(StatementTree statement) {
    if (statement.is(Tree.Kind.THROW_STATEMENT)) {
      return true;
    }

    if (statement.is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatement = (IfStatementTree) statement;
      return containsThrow(ifStatement.thenStatement());
    }

    if (statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      ExpressionTree expression = ((ExpressionStatementTree) statement).expression();
      if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
        return VALIDATION_METHODS.matches((MethodInvocationTree) expression);
      }
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
   * Check if statement contains field assignment (this.field = value).
   */
  private static boolean containsFieldAssignment(StatementTree statement) {
    FieldAssignmentFinder finder = new FieldAssignmentFinder();
    statement.accept(finder);
    return finder.foundFieldAssignment;
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
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      if (ExpressionUtils.isSelectOnThisOrSuper((tree))) {
        hasInstanceDependency = true;
        return;
      }

      super.visitMemberSelectExpression(tree);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      Symbol symbol = tree.symbol();

      // Allow parameters, local variables and static fields / methods
      if (symbol.isLocalVariable() || symbol.isStatic()|| parameters.contains(symbol)) {
        return;
      }

      // Check if it's an instance field or method being accessed
      if (symbol.isVariableSymbol() || symbol.isMethodSymbol()) {
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

  /**
   * Visitor to find field assignments.
   */
  private static class FieldAssignmentFinder extends BaseTreeVisitor {
    boolean foundFieldAssignment = false;

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      // Check if this is a field assignment (this.field = ...)
      if (ExpressionUtils.isThis(tree.expression())) {
        Tree parent = tree.parent();
        if (parent != null && parent.is(Tree.Kind.ASSIGNMENT)) {
          foundFieldAssignment = true;
          return;
        }
      }
      super.visitMemberSelectExpression(tree);
    }
  }
}
