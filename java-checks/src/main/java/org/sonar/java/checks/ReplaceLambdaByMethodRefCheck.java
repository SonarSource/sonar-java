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
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S1612",
  name = "Lambdas should be replaced with method references",
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
    if (isSingleMethodInvocationUsingLambdaParamAsArg(tree) || isBlockInvokingMethod(tree.body())) {
      context.addIssue(tree, this, "Replace this lambda with a method reference.");
    }
    super.visitLambdaExpression(tree);
  }

  private static boolean isSingleMethodInvocationUsingLambdaParamAsArg(LambdaExpressionTree tree) {
    List<VariableTree> parameters = tree.parameters();
    Tree body = tree.body();
    if (parameters.size() == 1 && body.is(Tree.Kind.METHOD_INVOCATION)) {
      List<IdentifierTree> usages = parameters.get(0).symbol().usages();
      Arguments arguments = ((MethodInvocationTree) body).arguments();
      return usages.size() == 1 && arguments.size() == 1 && usages.get(0).equals(arguments.get(0));
    }
    return false;
  }

  private static boolean isBlockInvokingMethod(Tree tree) {
    if (isBlockWithOneStatement(tree)) {
      Tree statement = ((BlockTree) tree).body().get(0);
      return isExpressionStatementInvokingMethod(statement) || isReturnStatementInvokingMethod(statement);
    }
    return false;
  }

  private static boolean isBlockWithOneStatement(Tree tree) {
    return tree.is(Tree.Kind.BLOCK) && ((BlockTree) tree).body().size() == 1;
  }

  private static boolean isExpressionStatementInvokingMethod(Tree statement) {
    return statement.is(Tree.Kind.EXPRESSION_STATEMENT) && isMethodInvocation(((ExpressionStatementTree) statement).expression());
  }

  private static boolean isReturnStatementInvokingMethod(Tree statement) {
    return statement.is(Tree.Kind.RETURN_STATEMENT) && isMethodInvocation(((ReturnStatementTree) statement).expression());
  }

  private static boolean isMethodInvocation(@Nullable Tree tree) {
    return tree != null && tree.is(Tree.Kind.METHOD_INVOCATION);
  }

}
