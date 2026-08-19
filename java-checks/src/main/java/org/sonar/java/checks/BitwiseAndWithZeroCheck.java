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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S9344")
public class BitwiseAndWithZeroCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove this bitwise AND with zero; the result is always zero.";

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.AND, Kind.AND_ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.AND)) {
      BinaryExpressionTree binary = (BinaryExpressionTree) tree;
      if (isZero(binary.leftOperand()) || isZero(binary.rightOperand())) {
        reportIssue(binary.operatorToken(), MESSAGE);
      }
    } else {
      AssignmentExpressionTree assignment = (AssignmentExpressionTree) tree;
      if (isZero(assignment.expression())) {
        reportIssue(assignment.operatorToken(), MESSAGE);
      }
    }
  }

  private static boolean isZero(ExpressionTree expression) {
    Long value = LiteralUtils.longLiteralValue(ExpressionUtils.skipParentheses(expression));
    return value != null && value == 0L;
  }

}
