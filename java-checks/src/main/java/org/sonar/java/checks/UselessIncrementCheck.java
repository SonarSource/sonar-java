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
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2123")
public class UselessIncrementCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.RETURN_STATEMENT, Tree.Kind.ASSIGNMENT, Tree.Kind.LAMBDA_EXPRESSION);
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
    } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
      AssignmentExpressionTree aet = (AssignmentExpressionTree) tree;
      if (isPostfix(aet.expression())) {
        UnaryExpressionTree postfix = (UnaryExpressionTree) aet.expression();
        if (SyntacticEquivalence.areEquivalent(aet.variable(), postfix.expression())) {
          reportIssue(postfix);
        }
      }
    } else {
      checkLambda((LambdaExpressionTree) tree);
    }
  }

  private void checkLambda(LambdaExpressionTree lambda) {
    Tree body = lambda.body();
    if (body instanceof ExpressionTree expressionTree) {
      body = ExpressionUtils.skipParentheses(expressionTree);
    }
    if (!isPostfix(body)) {
      return;
    }
    UnaryExpressionTree unary = (UnaryExpressionTree) body;
    if (lambda.symbol().returnType().type().isVoid()) {
      return;
    }
    ExpressionTree operand = ExpressionUtils.skipParentheses(unary.expression());
    if (!operand.is(Tree.Kind.IDENTIFIER)) {
      return;
    }
    Symbol operandSymbol = ((IdentifierTree) operand).symbol();
    boolean isParameter = lambda.parameters().stream()
      .map(VariableTree::symbol)
      .anyMatch(operandSymbol::equals);
    if (isParameter) {
      reportIssue(unary);
    }
  }

  private void reportIssue(UnaryExpressionTree expression) {
    reportIssue(expression.operatorToken(), "Remove this increment or correct the code not to waste it.");
  }

  private static boolean isPostfix(Tree tree) {
    return tree.is(Tree.Kind.POSTFIX_INCREMENT, Tree.Kind.POSTFIX_DECREMENT);
  }

}
