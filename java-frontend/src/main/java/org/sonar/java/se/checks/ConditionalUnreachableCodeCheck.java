/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.se.checks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S2583")
public class ConditionalUnreachableCodeCheck extends SECheck {

  public static final String MESSAGE = "Change this condition so that it does not always evaluate to \"%s\"";

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    CheckerContext.AlwaysTrueOrFalseExpressions atof = context.alwaysTrueOrFalseExpressions();
    for (Tree condition : atof.alwaysFalse()) {
      reportBooleanExpression(context, atof, condition, false);
    }
    for (Tree condition : atof.alwaysTrue()) {
      reportBooleanExpression(context, atof, condition, true);
    }
  }

  private void reportBooleanExpression(CheckerContext context, CheckerContext.AlwaysTrueOrFalseExpressions atof, Tree condition, boolean isTrue) {
    if (hasUnreachableCode(condition, isTrue)) {
      Set<List<JavaFileScannerContext.Location>> flows = atof.flowForExpression(condition).stream()
        .map(flow -> addIssueLocation(flow, condition, isTrue))
        .collect(Collectors.toSet());
      context.reportIssue(condition, this, String.format(MESSAGE, isTrue), flows);
    }
  }

  static boolean hasUnreachableCode(Tree booleanExpr, boolean isTrue) {
    Tree parent = biggestTreeWithSameEvaluation(booleanExpr, isTrue);
    if (parent.is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatementTree = (IfStatementTree) parent;
      return !isTrue || ifStatementTree.elseStatement() != null;
    }
    // Tree.Kind.DO_STATEMENT not considered, because it is always executed at least once
    if (parent.is(Tree.Kind.WHILE_STATEMENT) && !isTrue) {
      return true;
    }
    return parent.is(Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  private static Tree biggestTreeWithSameEvaluation(Tree booleanExpr, boolean isTrue) {
    Tree.Kind operator = isTrue ? Tree.Kind.CONDITIONAL_OR : Tree.Kind.CONDITIONAL_AND;
    Tree prevParent = booleanExpr;
    Tree parent = skipParentheses(booleanExpr.parent());
    while (parent != null && parent.is(operator)
      && equalsIgnoreParentheses(((BinaryExpressionTree) parent).leftOperand(), (ExpressionTree) prevParent)) {
      prevParent = parent;
      parent = skipParentheses(parent.parent());
    }
    Preconditions.checkState(parent != null, "Error getting parent tree with same evaluation, parent is null");
    return parent;
  }

  @CheckForNull
  private static Tree skipParentheses(@Nullable Tree tree) {
    Tree result = tree;
    while (result != null && result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      result = result.parent();
    }
    return result;
  }

  private static boolean equalsIgnoreParentheses(ExpressionTree tree, ExpressionTree other) {
    return skipParentheses(tree) == skipParentheses(other);
  }

  static List<JavaFileScannerContext.Location> addIssueLocation(List<JavaFileScannerContext.Location> flow, Tree issueTree, boolean conditionIsAlwaysTrue) {
    return ImmutableList.<JavaFileScannerContext.Location>builder()
      .add(new JavaFileScannerContext.Location("Expression is always " + conditionIsAlwaysTrue + ".", issueTree))
      .addAll(flow)
      .build();
  }
}
