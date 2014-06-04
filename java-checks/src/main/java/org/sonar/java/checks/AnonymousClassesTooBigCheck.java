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

import com.sonar.sslr.api.AstNode;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;

@Rule(
    key = AnonymousClassesTooBigCheck.RULE_KEY,
    priority = Priority.MAJOR,
    tags = {"brain-overload"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class AnonymousClassesTooBigCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1188";
  private static final RuleKey RULE = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);
  private static final int DEFAULT_MAX = 20;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX)
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
        context.addIssue(tree, RULE, "Reduce this anonymous class number of lines from " + lines + " to at most " + max + ", or make it a named class.");
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
    int lines = getNumberOfLines(((JavaTree) lambdaExpressionTree.body()).getAstNode());
    if (lines > max) {
      context.addIssue(lambdaExpressionTree, RULE, "Reduce this lambda expression number of lines from " + lines + " to at most " + max + ".");
    }
    super.visitLambdaExpression(lambdaExpressionTree);
  }

  private int getNumberOfLines(ClassTree classTree) {
    AstNode node = ((JavaTree) classTree).getAstNode();
    if(!node.is(JavaGrammar.CLASS_BODY)){
      node = node.getFirstChild(JavaGrammar.CLASS_BODY);
    }
    return getNumberOfLines(node);
  }

  private int getNumberOfLines(AstNode node) {
    return node.getLastChild().getTokenLine() - node.getTokenLine() + 1;
  }

}
