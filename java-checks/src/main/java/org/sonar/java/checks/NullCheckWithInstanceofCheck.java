/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S4201")
public class NullCheckWithInstanceofCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    BinaryExpressionTree binaryExpression = (BinaryExpressionTree) tree;
    ExpressionTree leftOp = ExpressionUtils.skipParentheses(binaryExpression.leftOperand());
    ExpressionTree rightOp = ExpressionUtils.skipParentheses(binaryExpression.rightOperand());
    if ((is(Tree.Kind.EQUAL_TO, leftOp, rightOp) && nullCheckWithInstanceOf(leftOp, rightOp, binaryExpression.kind(), Tree.Kind.CONDITIONAL_OR)) ||
      (is(Tree.Kind.NOT_EQUAL_TO, leftOp, rightOp) && nullCheckWithInstanceOf(leftOp, rightOp, binaryExpression.kind(), Tree.Kind.CONDITIONAL_AND))) {
      reportIssue(treeToReport(leftOp, rightOp), "Remove this unnecessary null check; \"instanceof\" returns false for nulls.");
    }
  }

  private static boolean nullCheckWithInstanceOf(ExpressionTree leftOp, ExpressionTree rightOp, Tree.Kind binaryExpressionKind, Tree.Kind expectedKind) {
    ExpressionTree binaryVariable = Optional.ofNullable(binaryExpressionVariable(leftOp))
      .orElse(binaryExpressionVariable(rightOp));
    if (binaryVariable == null || binaryExpressionKind != expectedKind) {
      return false;
    }
    ExpressionTree instanceofVariable = Optional.ofNullable(instanceofFound(rightOp, binaryExpressionKind))
      .orElse(instanceofFound(leftOp, binaryExpressionKind));
    return instanceofVariable != null && SyntacticEquivalence.areEquivalent(binaryVariable, instanceofVariable);
  }

  private static ExpressionTree treeToReport(ExpressionTree left, ExpressionTree right) {
    return left.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO) ? left : right;
  }

  @CheckForNull
  private static ExpressionTree binaryExpressionVariable(ExpressionTree expression) {
    BinaryExpressionTree binaryExpression = null;
    if (expression.is(Tree.Kind.NOT_EQUAL_TO, Tree.Kind.EQUAL_TO)) {
      binaryExpression = (BinaryExpressionTree) expression;
      if (binaryExpression.leftOperand().is(Tree.Kind.NULL_LITERAL)) {
        return binaryExpression.rightOperand();
      } else if (binaryExpression.rightOperand().is(Tree.Kind.NULL_LITERAL)) {
        return binaryExpression.leftOperand();
      }
    }
    return null;
  }

  @CheckForNull
  private static ExpressionTree instanceofFound(ExpressionTree expressionTree, Tree.Kind kind) {
    if (kind == Tree.Kind.CONDITIONAL_OR) {
      /* if CONDITIONAL_OR we want LOGICAL COMPLEMENT before instanceof */
      if (expressionTree.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
        return instanceofLHS(ExpressionUtils.skipParentheses(((UnaryExpressionTree) expressionTree).expression()));
      } else {
        return null;
      }
    } else {
      return instanceofLHS(expressionTree);
    }
  }

  @CheckForNull
  private static ExpressionTree instanceofLHS(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.INSTANCE_OF)) {
      return ((InstanceOfTree) expressionTree).expression();
    }
    return null;
  }

  private static boolean is(Tree.Kind kind, Tree... trees) {
    return Arrays.stream(trees).anyMatch(tree -> tree.is(kind));
  }
}
