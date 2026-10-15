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

@Rule(key = "S9433")
public class LiteralToStringByConcatenationCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Use an explicit conversion instead of concatenating this literal with an empty string.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.PLUS);
  }

  @Override
  public void visitNode(Tree tree) {
    BinaryExpressionTree binaryExpression = (BinaryExpressionTree) tree;
    ExpressionTree leftOperand = ExpressionUtils.skipParentheses(binaryExpression.leftOperand());
    ExpressionTree rightOperand = ExpressionUtils.skipParentheses(binaryExpression.rightOperand());
    if ((isEmptyStringLiteral(leftOperand) && isPrimitiveLiteral(rightOperand))
      || (isPrimitiveLiteral(leftOperand) && isEmptyStringLiteral(rightOperand))) {
      reportIssue(binaryExpression, MESSAGE);
    }
  }

  private static boolean isEmptyStringLiteral(ExpressionTree tree) {
    return tree.is(Tree.Kind.STRING_LITERAL) && tree.asConstant(String.class).filter(String::isEmpty).isPresent();
  }

  private static boolean isPrimitiveLiteral(ExpressionTree tree) {
    return tree.is(
      Tree.Kind.BOOLEAN_LITERAL,
      Tree.Kind.CHAR_LITERAL,
      Tree.Kind.INT_LITERAL,
      Tree.Kind.LONG_LITERAL,
      Tree.Kind.FLOAT_LITERAL,
      Tree.Kind.DOUBLE_LITERAL);
  }
}
