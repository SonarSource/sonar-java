/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "S881")
public class IncrementDecrementInSubExpressionCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    scanSkippingIncrementOrDecrement(tree.expression());
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    scan(lambdaExpressionTree.parameters());
    scanSkippingIncrementOrDecrement(lambdaExpressionTree.body());
  }

  private void scanSkippingIncrementOrDecrement(Tree expressionTree) {
    if (isIncrementOrDecrement(expressionTree)) {
      UnaryExpressionTree unaryExpressionTree = (UnaryExpressionTree) expressionTree;
      expressionTree = unaryExpressionTree.expression();
    }
    scan(expressionTree);
  }

  @Override
  public void visitUnaryExpression(UnaryExpressionTree tree) {
    super.visitUnaryExpression(tree);
    if (isIncrementOrDecrement(tree)) {
      context.reportIssue(this, tree, "Extract this increment or decrement operator into a dedicated statement.");
    }
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    ExpressionTree expression = tree.expression();
    if(expression == null || !isIncrementOrDecrement(expression)) {
      scan(expression);
    }
  }

  private static boolean isIncrementOrDecrement(Tree tree) {
    return tree.is(Kind.PREFIX_INCREMENT) ||
      tree.is(Kind.PREFIX_DECREMENT) ||
      tree.is(Kind.POSTFIX_INCREMENT) ||
      tree.is(Kind.POSTFIX_DECREMENT);
  }

}
