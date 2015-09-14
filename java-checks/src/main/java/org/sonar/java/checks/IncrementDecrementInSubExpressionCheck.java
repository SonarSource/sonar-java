/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S881",
  name = "Increment (++) and decrement (--) operators should not be mixed with other operators in an expression",
  tags = {"cert", "misra"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
public class IncrementDecrementInSubExpressionCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    ExpressionTree expressionTree = tree.expression();
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
      context.addIssue(tree, this, "Extract this increment or decrement operator into a dedicated statement.");
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
