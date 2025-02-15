/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
