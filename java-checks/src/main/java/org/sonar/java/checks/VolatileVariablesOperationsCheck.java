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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S3078")
public class VolatileVariablesOperationsCheck extends IssuableSubscriptionVisitor {

  private final Set<Tree> visitedUnaryExpressions = new HashSet<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(
      Tree.Kind.PREFIX_DECREMENT,
      Tree.Kind.PREFIX_INCREMENT,
      Tree.Kind.POSTFIX_DECREMENT,
      Tree.Kind.POSTFIX_INCREMENT,
      Tree.Kind.ASSIGNMENT,
      Tree.Kind.PLUS_ASSIGNMENT,
      Tree.Kind.MINUS_ASSIGNMENT,
      Tree.Kind.MULTIPLY_ASSIGNMENT,
      Tree.Kind.DIVIDE_ASSIGNMENT,
      Tree.Kind.REMAINDER_ASSIGNMENT,
      Tree.Kind.LEFT_SHIFT_ASSIGNMENT,
      Tree.Kind.RIGHT_SHIFT_ASSIGNMENT,
      Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
      Tree.Kind.AND_ASSIGNMENT,
      Tree.Kind.XOR_ASSIGNMENT,
      Tree.Kind.OR_ASSIGNMENT
    );
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof UnaryExpressionTree unaryExpression) {
      checkUnaryExpression(unaryExpression);
    } else {
      checkAssignment((AssignmentExpressionTree) tree);
    }
  }

  private void checkUnaryExpression(UnaryExpressionTree unaryExpression) {
    if (visitedUnaryExpressions.contains(unaryExpression)) {
      return;
    }
    IdentifierTree identifier = getVariableIdentifier(ExpressionUtils.skipParentheses(unaryExpression.expression()));
    if (identifier == null || !identifier.symbol().isVolatile()) {
      return;
    }
    reportIssueIfNotInExcludedContext(identifier);
  }

  private void checkAssignment(AssignmentExpressionTree assignment) {
    IdentifierTree assignee = getVariableIdentifier(assignment.variable());
    if (assignee == null) {
      return;
    }

    Symbol assigneeSymbol = assignee.symbol();
    if (!assigneeSymbol.isVolatile()) {
      return;
    }

    if (assignment.is(Tree.Kind.ASSIGNMENT)) {
      SymbolCollector symbolCollector = new SymbolCollector();
      assignment.expression().accept(symbolCollector);
      Set<Symbol> symbols = symbolCollector.symbols;
      if (!symbols.contains(assigneeSymbol)) {
        return;
      }
    }

    reportIssueIfNotInExcludedContext(assignee);
  }

  @Nullable
  private static IdentifierTree getVariableIdentifier(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      return (IdentifierTree) expressionTree;
    }
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) expressionTree).identifier();
    }
    return null;
  }

  private void reportIssueIfNotInExcludedContext(IdentifierTree identifier) {
    Optional<String> recommendedType = recommendedType(identifier);
    if (recommendedType.isEmpty()) {
      return;
    }

    Tree current = identifier.parent();
    boolean foundClass = false;
    while (!foundClass) {
      switch (current.kind()) {
        case LAMBDA_EXPRESSION, SYNCHRONIZED_STATEMENT, COMPILATION_UNIT:
          return;
        case METHOD:
          if (ModifiersUtils.hasModifier(((MethodTree) current).modifiers(), Modifier.SYNCHRONIZED)) {
            return;
          }
          break;
        case ENUM, CLASS, INTERFACE, RECORD, IMPLICIT_CLASS:
          if (!current.is(Tree.Kind.IMPLICIT_CLASS) && ((ClassTree) current).simpleName() == null) {
            return;
          }
          // we got to a non-anonymous class or implicit class in a compact source file, we can safely raise an issue
          foundClass = true;
          break;
      }
      current = current.parent();
    }

    reportIssue(identifier, String.format("Use an \"%s\" for this field; its operations are atomic.", recommendedType.get()));
  }

  private static Optional<String> recommendedType(IdentifierTree identifier) {
    Type type = identifier.symbol().type();
    if (type.is("boolean") || type.is("java.lang.Boolean")) {
      return Optional.of("AtomicBoolean");
    } else if (type.is("int") || type.is("java.lang.Integer")) {
      return Optional.of("AtomicInteger");
    } else if (type.is("long") || type.is("java.lang.Long")) {
      return Optional.of("AtomicLong");
    } else {
      return Optional.empty();
    }
  }

  private class SymbolCollector extends BaseTreeVisitor {

    final Set<Symbol> symbols = new HashSet<>();

    @Override
    public void visitUnaryExpression(UnaryExpressionTree tree) {
      visitedUnaryExpressions.add(tree);
      super.visitUnaryExpression(tree);
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      symbols.add(tree.symbol());
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      symbols.add(tree.identifier().symbol());
      super.visitMemberSelectExpression(tree);
      visitedUnaryExpressions.clear();
    }

  }

}
