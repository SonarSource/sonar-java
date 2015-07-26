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
import org.sonar.check.RuleProperty;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.java.syntaxtoken.LastSyntaxTokenFinder;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S1188",
  name = "Lambdas and anonymous classes should not have too many lines",
  tags = {"java8"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("20min")
public class AnonymousClassesTooBigCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_MAX = 20;

  @RuleProperty(key = "Max",
    defaultValue = "" + DEFAULT_MAX,
    description = "Maximum allowed lines in an anonymous class/lambda")
  public int max = DEFAULT_MAX;

  private JavaFileScannerContext context;
  /**
   * Flag to skip check for class bodies of EnumConstants.
   */
  private boolean isEnumConstantBody;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    isEnumConstantBody = false;
    scan(context.getTree());
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.classBody() != null && !isEnumConstantBody) {
      int lines = getNumberOfLines(tree.classBody());
      if (lines > max) {
        context.addIssue(tree, this, "Reduce this anonymous class number of lines from " + lines + " to at most " + max + ", or make it a named class.");
      }
    }
    isEnumConstantBody = false;
    super.visitNewClass(tree);
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    isEnumConstantBody = true;
    super.visitEnumConstant(tree);
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    int lines = getNumberOfLines(lambdaExpressionTree);
    if (lines > max) {
      context.addIssue(lambdaExpressionTree, this, "Reduce this lambda expression number of lines from " + lines + " to at most " + max + ".");
    }
    super.visitLambdaExpression(lambdaExpressionTree);
  }

  private static int getNumberOfLines(ClassTree classTree) {
    int startLine = classTree.openBraceToken().line();
    int endline = classTree.closeBraceToken().line();
    return endline - startLine + 1;
  }

  private static int getNumberOfLines(LambdaExpressionTree lambdaExpressionTree) {
    Tree body = lambdaExpressionTree.body();
    SyntaxToken firstSyntaxToken = FirstSyntaxTokenFinder.firstSyntaxToken(body);
    SyntaxToken lastSyntaxToken = LastSyntaxTokenFinder.lastSyntaxToken(body);
    if (firstSyntaxToken == null || lastSyntaxToken == null) {
      // Only happen if the body of the lambda expression is a Tree.Kind.OTHER
      return 0;
    }
    return lastSyntaxToken.line() - firstSyntaxToken.line() + 1;
  }

}
