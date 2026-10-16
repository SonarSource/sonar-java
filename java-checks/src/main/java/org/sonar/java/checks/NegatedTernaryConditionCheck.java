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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S9435")
public class NegatedTernaryConditionCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Invert this negated condition and swap the two branches of the ternary.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CONDITIONAL_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree condition = ((ConditionalExpressionTree) tree).condition();
    if (isNegated(ExpressionUtils.skipParentheses(condition))) {
      reportIssue(condition, MESSAGE);
    }
  }

  private static boolean isNegated(ExpressionTree condition) {
    if (condition.is(Tree.Kind.LOGICAL_COMPLEMENT)) {
      ExpressionTree negated = ExpressionUtils.skipParentheses(((UnaryExpressionTree) condition).expression());
      // "!(value == null)" is a null check spelled the long way, and is exempt like "value != null".
      return !isNullComparison(negated, Tree.Kind.EQUAL_TO);
    }
    return isNotEqualButNotNullCheck(condition);
  }

  private static boolean isNotEqualButNotNullCheck(ExpressionTree condition) {
    return condition.is(Tree.Kind.NOT_EQUAL_TO) && !isNullComparison(condition, Tree.Kind.NOT_EQUAL_TO);
  }

  private static boolean isNullComparison(ExpressionTree expression, Tree.Kind kind) {
    if (!expression.is(kind)) {
      return false;
    }
    BinaryExpressionTree comparison = (BinaryExpressionTree) expression;
    return isNullLiteral(comparison.leftOperand()) || isNullLiteral(comparison.rightOperand());
  }

  private static boolean isNullLiteral(ExpressionTree expression) {
    return ExpressionUtils.skipParentheses(expression).is(Tree.Kind.NULL_LITERAL);
  }
}
