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

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S2123")
public class UselessIncrementCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.RETURN_STATEMENT, Tree.Kind.ASSIGNMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.RETURN_STATEMENT)) {
      ExpressionTree returnExpression = ((ReturnStatementTree) tree).expression();
      if (returnExpression != null && isPostfix(returnExpression)) {
        UnaryExpressionTree unaryExpression = (UnaryExpressionTree) returnExpression;
        ExpressionTree expression = ExpressionUtils.skipParentheses(unaryExpression.expression());
        if (expression.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) expression).symbol().owner().isMethodSymbol()) {
          reportIssue(unaryExpression);
        }
      }
    } else {
      AssignmentExpressionTree aet = (AssignmentExpressionTree) tree;
      if (isPostfix(aet.expression())) {
        UnaryExpressionTree postfix = (UnaryExpressionTree) aet.expression();
        if (SyntacticEquivalence.areEquivalent(aet.variable(), postfix.expression())) {
          reportIssue(postfix);
        }
      }
    }
  }

  private void reportIssue(UnaryExpressionTree expression) {
    reportIssue(expression.operatorToken(), "Remove this increment or correct the code not to waste it.");
  }

  private static boolean isPostfix(ExpressionTree tree) {
    return tree.is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT);
  }

}
