/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S1612",
  name = "Replace lambdas with method references when possible",
  tags = {"java8"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class ReplaceLambdaByMethodRefCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree tree) {
    if (isMethodInvocation(tree.body()) || isBlockInvokingMethod(tree.body())) {
      context.addIssue(tree, this, "Replace this lambda with a method reference.");
    }
    super.visitLambdaExpression(tree);
  }

  private boolean isMethodInvocation(Tree tree) {
    return tree != null && tree.is(Tree.Kind.METHOD_INVOCATION);
  }

  private boolean isBlockInvokingMethod(Tree tree) {
    if (isBlockWithOneStatement(tree)) {
      Tree statement = ((BlockTree) tree).body().get(0);
      return isExpressionStatementInvokingMethod(statement) || isReturnStatementInvokingMethod(statement);
    }
    return false;
  }

  private boolean isReturnStatementInvokingMethod(Tree statement) {
    return statement.is(Tree.Kind.RETURN_STATEMENT) && isMethodInvocation(((ReturnStatementTree) statement).expression());
  }

  private boolean isExpressionStatementInvokingMethod(Tree statement) {
    return statement.is(Tree.Kind.EXPRESSION_STATEMENT) && isMethodInvocation(((ExpressionStatementTree) statement).expression());
  }

  private boolean isBlockWithOneStatement(Tree tree) {
    return tree.is(Tree.Kind.BLOCK) && ((BlockTree) tree).body().size() == 1;
  }

}
