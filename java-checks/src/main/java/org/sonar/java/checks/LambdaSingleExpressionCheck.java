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

import com.google.common.annotations.Beta;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1602",
  name = "Lamdbas containing only one statement should not nest this statement in a block",
  tags = {"java8"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@Beta
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
public class LambdaSingleExpressionCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    if (isBlockWithOneStatement(lambdaExpressionTree.body())) {
      String message = "Remove useless curly braces around statement";
      if (singleStatementIsReturn(lambdaExpressionTree)) {
        message += " and then remove useless return keyword";
      }
      context.addIssue(lambdaExpressionTree.body(), this, message);
    }
    super.visitLambdaExpression(lambdaExpressionTree);
  }

  private boolean isBlockWithOneStatement(Tree tree) {
    boolean result = false;
    if (tree.is(Tree.Kind.BLOCK)) {
      List<StatementTree> blockBody = ((BlockTree) tree).body();
      result = blockBody.size() == 1 && isRefactorizable(blockBody.get(0));
    }
    return result;
  }

  private boolean isRefactorizable(StatementTree statementTree) {
    return isBlockWithOneStatement(statementTree) || statementTree.is(Tree.Kind.EXPRESSION_STATEMENT) || isReturnStatement(statementTree);
  }

  private boolean singleStatementIsReturn(LambdaExpressionTree lambdaExpressionTree) {
    return isReturnStatement(((BlockTree) lambdaExpressionTree.body()).body().get(0));
  }

  private boolean isReturnStatement(Tree tree){
    return tree.is(Tree.Kind.RETURN_STATEMENT);
  }

}
