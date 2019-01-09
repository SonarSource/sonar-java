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

import com.google.common.collect.Lists;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.visitors.LinesOfCodeVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1188")
public class AnonymousClassesTooBigCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_MAX = 20;

  @RuleProperty(key = "Max",
    description = "Maximum allowed lines in an anonymous class/lambda",
    defaultValue = "" + DEFAULT_MAX)
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
        context.reportIssue(this, tree.newKeyword(), tree.identifier(),
          "Reduce this anonymous class number of lines from " + lines + " to at most " + max + ", or make it a named class.");
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
      SyntaxToken firstToken = lambdaExpressionTree.firstToken();
      SyntaxToken lastSyntaxToken = lambdaExpressionTree.lastToken();
      JavaFileScannerContext.Location lastTokenLocation = new JavaFileScannerContext.Location(lines + " lines", lastSyntaxToken);
      context.reportIssue(this, firstToken, lambdaExpressionTree.arrowToken(),
        "Reduce this lambda expression number of lines from " + lines + " to at most " + max + ".", Lists.newArrayList(lastTokenLocation), null);
    }
    super.visitLambdaExpression(lambdaExpressionTree);
  }

  private static int getNumberOfLines(Tree tree) {
    return new LinesOfCodeVisitor().linesOfCode(tree);
  }
}
