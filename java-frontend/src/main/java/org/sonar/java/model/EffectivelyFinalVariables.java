/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Marks the local variables and parameters that are never reassigned (JLS 4.12.4), as the compiler does.
 * A variable declared without a value stays effectively final after one plain assignment.
 */
final class EffectivelyFinalVariables extends BaseTreeVisitor {

  private final Map<Symbol, int[]> assignments = new IdentityHashMap<>();
  private final List<VariableTree> variables = new ArrayList<>();

  private EffectivelyFinalVariables() {
  }

  static void mark(JavaTree.CompilationUnitTreeImpl unit) {
    EffectivelyFinalVariables visitor = new EffectivelyFinalVariables();
    visitor.scan(unit);
    for (VariableTree variable : visitor.variables) {
      if (variable.symbol() instanceof JVariableSymbol symbol) {
        int[] counts = visitor.assignments.getOrDefault(symbol, new int[2]);
        boolean unassigned = variable.initializer() == null && isLocalDeclaration(variable);
        symbol.effectivelyFinal(counts[1] == 0 && counts[0] <= (unassigned ? 1 : 0));
      }
    }
  }

  private static boolean isLocalDeclaration(VariableTree variable) {
    Tree parent = variable.parent();
    return parent != null && parent.is(Tree.Kind.BLOCK, Tree.Kind.CASE_GROUP, Tree.Kind.STATIC_INITIALIZER, Tree.Kind.INITIALIZER, Tree.Kind.FOR_STATEMENT);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    if (tree.symbol() instanceof JVariableSymbol symbol && (symbol.isLocalVariable() || symbol.isParameter())) {
      variables.add(tree);
    }
    super.visitVariable(tree);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    record(tree.variable(), tree.is(Tree.Kind.ASSIGNMENT) ? 0 : 1);
    super.visitAssignmentExpression(tree);
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    if (tree.is(Tree.Kind.PREFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT)) {
      record(tree.expression(), 1);
    }
    super.visitUnaryExpression(tree);
  }

  private void record(ExpressionTree target, int slot) {
    ExpressionTree expression = target;
    while (expression instanceof org.sonar.plugins.java.api.tree.ParenthesizedTree parenthesized) {
      expression = parenthesized.expression();
    }
    if (expression instanceof IdentifierTree identifier) {
      assignments.computeIfAbsent(identifier.symbol(), key -> new int[2])[slot]++;
    }
  }
}
