/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

@Rule(key = "S3973")
public class IndentationAfterConditionalCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  private Set<IfStatementTree> statementSet = new HashSet<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    statementSet.clear();
    scan(context.getTree());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    visitIfElseStatement(tree, tree.firstToken().column());
    super.visitIfStatement(tree);
  }

  /* to traverse the if-else if statements, in case of else if */
  public void visitIfElseStatement(IfStatementTree tree, int column) {
    if (!statementSet.contains(tree)) {
      statementSet.add(tree);
      checkForReport(tree.thenStatement(), tree.ifKeyword(), tree.closeParenToken(), column);
      SyntaxToken elseKeyword = tree.elseKeyword();
      if (elseKeyword != null) {
        StatementTree elseStatement = tree.elseStatement();
        if (elseStatement.is(Tree.Kind.IF_STATEMENT)) {
          visitIfElseStatement((IfStatementTree) elseStatement, elseKeyword.column());
        } else {
          checkElseStatementReport(tree);
        }
      }
    }
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    super.visitWhileStatement(tree);
    checkForReport(tree.statement(), tree.whileKeyword(), tree.closeParenToken(), tree.firstToken().column());
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    super.visitForStatement(tree);
    checkForReport(tree.statement(), tree.forKeyword(), tree.closeParenToken(), tree.firstToken().column());
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    super.visitForEachStatement(tree);
    checkForReport(tree.statement(), tree.forKeyword(), tree.closeParenToken(), tree.firstToken().column());
  }

  public void checkForReport(StatementTree statement, Tree startTree, Tree endTree, int column) {
    if (noIssue(statement, column)) {
      return;
    }
    report(startTree, endTree, startTree.firstToken().text(), statement);
  }

  private static boolean noIssue(StatementTree statement, int column) {
    return (blockExists(statement) || isIndentationCorrect(statement, column));
  }

  public void report(Tree firstTree, Tree secondTree, String operationName, StatementTree statement) {
    context.reportIssue(this, firstTree, secondTree,
      "Use curly braces or indentation to denote the code conditionally executed by this \"" + operationName + "\".",
      Collections.singletonList(new JavaFileScannerContext.Location("", statement)), null);
  }

  public void checkElseStatementReport(IfStatementTree tree) {
    SyntaxToken elseKeyword = tree.elseKeyword();
    StatementTree elseStatement = tree.elseStatement();
    if (noIssue(elseStatement, elseKeyword.column())) {
      return;
    }
    context.reportIssue(this, elseKeyword,
      "Use curly braces or indentation to denote the code conditionally executed by this \"" + elseKeyword.text() + "\".",
      Collections.singletonList(new JavaFileScannerContext.Location("", elseStatement)), null);
  }

  private static int columnStatement(Tree tree) {
    return tree.firstToken().column();
  }

  private static boolean isIndentationCorrect(StatementTree statement, int column) {
    return columnStatement(statement) > column;
  }

  private static boolean blockExists(StatementTree statementTree) {
    return statementTree.is(Tree.Kind.BLOCK);
  }
}
