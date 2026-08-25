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
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9360")
public class YodaConditionCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Put the variable on the left side of this comparison.";
  private static final String MESSAGE_WITH_REVERSE = "Put the variable on the left side of this comparison and reverse the operator.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(
      Tree.Kind.EQUAL_TO,
      Tree.Kind.NOT_EQUAL_TO,
      Tree.Kind.LESS_THAN,
      Tree.Kind.GREATER_THAN,
      Tree.Kind.LESS_THAN_OR_EQUAL_TO,
      Tree.Kind.GREATER_THAN_OR_EQUAL_TO
    );
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryExpression = (BinaryExpressionTree) tree;
    ExpressionTree left = ExpressionUtils.skipParentheses(binaryExpression.leftOperand());
    ExpressionTree right = ExpressionUtils.skipParentheses(binaryExpression.rightOperand());

    if (isLiteral(left) && !isLiteral(right)) {
      reportIssue(left, isRelationalOperator(tree) ? MESSAGE_WITH_REVERSE : MESSAGE);
    }
  }

  private static boolean isRelationalOperator(Tree tree) {
    return tree.is(
      Tree.Kind.LESS_THAN,
      Tree.Kind.GREATER_THAN,
      Tree.Kind.LESS_THAN_OR_EQUAL_TO,
      Tree.Kind.GREATER_THAN_OR_EQUAL_TO
    );
  }

  private static boolean isLiteral(ExpressionTree tree) {
    return tree.is(
      Tree.Kind.INT_LITERAL,
      Tree.Kind.LONG_LITERAL,
      Tree.Kind.FLOAT_LITERAL,
      Tree.Kind.DOUBLE_LITERAL,
      Tree.Kind.BOOLEAN_LITERAL,
      Tree.Kind.CHAR_LITERAL,
      Tree.Kind.STRING_LITERAL,
      Tree.Kind.NULL_LITERAL
    );
  }
}
