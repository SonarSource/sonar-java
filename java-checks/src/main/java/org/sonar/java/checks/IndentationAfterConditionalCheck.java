/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import java.util.Collections;
import org.sonar.check.Rule;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
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

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    Tree parentTree = tree.parent();
    if (parentTree.is(Tree.Kind.IF_STATEMENT) && tree.equals(((IfStatementTree) parentTree).elseStatement())) {
      checkForReport(tree.thenStatement(), getAcceptableStartOfElse((IfStatementTree) parentTree), tree.closeParenToken(), tree.ifKeyword().firstToken().text());
    } else {
      checkForReport(tree.thenStatement(), tree.ifKeyword(), tree.closeParenToken(), tree.ifKeyword().text());
    }
    SyntaxToken elseKeyword = tree.elseKeyword();
    if (elseKeyword != null && !tree.elseStatement().is(Tree.Kind.IF_STATEMENT)) {
      checkForReport(tree.elseStatement(), getAcceptableStartOfElse(tree), elseKeyword, elseKeyword.firstToken().text());
    }
    super.visitIfStatement(tree);
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    super.visitWhileStatement(tree);
    checkForReport(tree.statement(), tree.whileKeyword(), tree.closeParenToken(), tree.whileKeyword().text());
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    super.visitForStatement(tree);
    checkForReport(tree.statement(), tree.forKeyword(), tree.closeParenToken(), tree.forKeyword().text());
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    super.visitForEachStatement(tree);
    checkForReport(tree.statement(), tree.forKeyword(), tree.closeParenToken(), tree.forKeyword().text());
  }

  private static Tree getAcceptableStartOfElse(IfStatementTree ifStatement) {
    StatementTree thenStatement = ifStatement.thenStatement();
    SyntaxToken elseKeyword = ifStatement.elseKeyword();
    if (thenStatement.is(Tree.Kind.BLOCK)) {
      SyntaxToken closeBrace = ((BlockTree) thenStatement).closeBraceToken();
      if (LineUtils.startLine(closeBrace) == LineUtils.startLine(elseKeyword)) {
        return closeBrace;
      }
    }
    return elseKeyword;
  }

  private void checkForReport(StatementTree statement, Tree startTree, Tree endTree, String name) {
    if (!(statement.is(Tree.Kind.BLOCK) ||
      Position.startOf(statement).column() > Position.startOf(startTree).column())) {
      context.reportIssue(this, startTree, endTree,
        "Use indentation to denote the code conditionally executed by this \"" + name + "\".",
        Collections.singletonList(new JavaFileScannerContext.Location("", statement)), null);
    }
  }
}
