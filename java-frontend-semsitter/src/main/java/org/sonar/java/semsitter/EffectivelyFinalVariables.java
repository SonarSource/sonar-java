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
package org.sonar.java.semsitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.SwitchTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Marks the local variables and parameters that are never reassigned (JLS 4.12.4), as the compiler does.
 * A local declared without a value stays effectively final when each plain assignment to it happens while it is
 * definitely unassigned, approximated as: no assignment repeats in a loop of its scope, and any two assignments sit
 * in exclusive branches, the two arms of an {@code if} or two arrow cases of a {@code switch}.
 */
final class EffectivelyFinalVariables extends BaseTreeVisitor {

  private final Map<Symbol, List<Tree>> plainAssignments = new IdentityHashMap<>();
  private final Set<Symbol> updated = Collections.newSetFromMap(new IdentityHashMap<>());
  private final List<VariableTree> variables = new ArrayList<>();

  private EffectivelyFinalVariables() {
  }

  static void mark(JavaTree.CompilationUnitTreeImpl unit) {
    EffectivelyFinalVariables visitor = new EffectivelyFinalVariables();
    visitor.scan(unit);
    for (VariableTree variable : visitor.variables) {
      JVariableSymbol symbol = (JVariableSymbol) variable.symbol();
      List<Tree> assignments = visitor.plainAssignments.getOrDefault(symbol, List.of());
      boolean unassigned = variable.initializer() == null && isLocalDeclaration(variable);
      boolean effectivelyFinal = !visitor.updated.contains(symbol) && (assignments.isEmpty() || (unassigned && assignedOnce(variable, assignments)));
      symbol.effectivelyFinal(effectivelyFinal);
    }
  }

  static boolean isLocalDeclaration(VariableTree variable) {
    Tree parent = variable.parent();
    return parent.is(Tree.Kind.BLOCK, Tree.Kind.CASE_GROUP, Tree.Kind.STATIC_INITIALIZER, Tree.Kind.INITIALIZER)
      || (parent.is(Tree.Kind.LIST) && parent.parent() != null && parent.parent().is(Tree.Kind.FOR_STATEMENT));
  }

  static boolean assignedOnce(VariableTree variable, List<Tree> assignments) {
    if (assignments.stream().anyMatch(assignment -> repeatsInLoop(assignment, variable.parent()))) {
      return false;
    }
    for (int i = 0; i < assignments.size(); i++) {
      for (int j = i + 1; j < assignments.size(); j++) {
        if (!exclusive(assignments.get(i), assignments.get(j))) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean repeatsInLoop(Tree assignment, Tree scope) {
    for (Tree current = assignment.parent(); current != null && current != scope; current = current.parent()) {
      if (current.is(Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT, Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT)) {
        return true;
      }
    }
    return false;
  }

  /** Whether at most one of {@code first} and {@code second} runs: they sit in two arms of one branching. */
  private static boolean exclusive(Tree first, Tree second) {
    Map<Tree, Tree> firstPath = new IdentityHashMap<>();
    for (Tree child = first, current = first.parent(); current != null; child = current, current = current.parent()) {
      firstPath.put(current, child);
    }
    Tree child = second;
    Tree common = second.parent();
    while (common != null && !firstPath.containsKey(common)) {
      child = common;
      common = common.parent();
    }
    if (common instanceof IfStatementTree branch) {
      Tree other = firstPath.get(common);
      return child != other && branch.elseStatement() != null && Set.of(branch.thenStatement(), branch.elseStatement()).equals(Set.of(child, other));
    }
    return common instanceof SwitchTree && isArrowCase(child) && isArrowCase(firstPath.get(common)) && child != firstPath.get(common);
  }

  private static boolean isArrowCase(Tree tree) {
    return tree instanceof CaseGroupTree group && group.labels().stream().noneMatch(CaseLabelTree::isFallThrough);
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
    Symbol target = assigned(tree.variable());
    if (target != null && tree.is(Tree.Kind.ASSIGNMENT)) {
      plainAssignments.computeIfAbsent(target, key -> new ArrayList<>()).add(tree);
    } else if (target != null) {
      updated.add(target);
    }
    super.visitAssignmentExpression(tree);
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    Symbol target = assigned(tree.expression());
    if (target != null && tree.is(Tree.Kind.PREFIX_INCREMENT, Tree.Kind.PREFIX_DECREMENT, Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT)) {
      updated.add(target);
    }
    super.visitUnaryExpression(tree);
  }

  @Nullable
  private static Symbol assigned(ExpressionTree target) {
    return ExpressionUtils.skipParentheses(target) instanceof IdentifierTree identifier ? identifier.symbol() : null;
  }
}
