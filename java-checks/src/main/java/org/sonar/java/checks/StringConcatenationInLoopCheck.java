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

import org.sonar.api.rule.RuleKey;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

@Rule(
    key = StringConcatenationInLoopCheck.RULE_KEY,
    priority = Priority.MAJOR,
    tags = {"performance"})
public class StringConcatenationInLoopCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1643";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private JavaFileScannerContext context;
  private int loopLevel;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    loopLevel = 0;
    scan(context.getTree());
  }


  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (loopLevel > 0 && isStringConcatenation(tree)) {
      context.addIssue(tree, ruleKey, "Use a StringBuilder instead.");
    }
    super.visitAssignmentExpression(tree);
  }

  private boolean isStringConcatenation(AssignmentExpressionTree tree) {
    return isString(((AssignmentExpressionTreeImpl) tree).getType()) && isConcatenation(tree);
  }

  private boolean isConcatenation(AssignmentExpressionTree tree) {
    return tree.is(Tree.Kind.PLUS_ASSIGNMENT) || (tree.is(Tree.Kind.ASSIGNMENT) && removeParenthesis(tree.expression()).is(Tree.Kind.PLUS));
  }

  private Tree removeParenthesis(Tree tree) {
    Tree result = tree;
    while(result.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      result = ((ParenthesizedTree) result).expression();
    }
    return result;
  }

  private boolean isString(Type type) {
    if (type.isTagged(Type.CLASS)) {
      Symbol.TypeSymbol typeSymbol = ((Type.ClassType) type).getSymbol();
      return "String".equals(typeSymbol.getName()) && "java.lang".equals(typeSymbol.owner().getName());
    }
    return false;
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    loopLevel++;
    super.visitForEachStatement(tree);
    loopLevel--;
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    loopLevel++;
    super.visitForStatement(tree);
    loopLevel--;
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    loopLevel++;
    super.visitWhileStatement(tree);
    loopLevel--;
  }

  @Override
  public void visitDoWhileStatement(DoWhileStatementTree tree) {
    loopLevel++;
    super.visitDoWhileStatement(tree);
    loopLevel--;
  }


}
