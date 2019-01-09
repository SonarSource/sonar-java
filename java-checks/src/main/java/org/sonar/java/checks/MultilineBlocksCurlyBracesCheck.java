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

import java.util.Locale;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

@Rule(key = "S2681")
public class MultilineBlocksCurlyBracesCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String LOOP_MESSAGE = "This line will not be executed in a loop; only the first line of this %d-line block will be. The rest will execute only once.";
  private static final String LOOP_MESSAGE_ONE_LINER = "This statement will not be executed in a loop; only the first statement will be. The rest will execute only once.";
  private static final String IF_MESSAGE = "This line will not be executed conditionally; " +
    "only the first line of this %d-line block will be. The rest will execute unconditionally.";
  private static final String IF_MESSAGE_ONE_LINER = "This statement will not be executed conditionally; only the first statement will be. The rest will execute unconditionally.";
  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitBlock(BlockTree tree) {
    super.visitBlock(tree);
    StatementTree previous = null;
    for (StatementTree current : tree.body()) {
      if (previous != null) {
        check(current, previous);
      }
      previous = current;
    }
  }

  private void check(StatementTree current, StatementTree previous) {
    StatementTree block = null;
    boolean condition = false;
    if (previous.is(Tree.Kind.FOR_EACH_STATEMENT)) {
      block = ((ForEachStatement) previous).statement();
    } else if (previous.is(Tree.Kind.FOR_STATEMENT)) {
      block = ((ForStatementTree) previous).statement();
    } else if (previous.is(Tree.Kind.WHILE_STATEMENT)) {
      block = ((WhileStatementTree) previous).statement();
    } else if (previous.is(Tree.Kind.IF_STATEMENT)) {
      block = getIfStatementLastBlock(previous);
      condition = true;
    }
    if (block != null && !block.is(Tree.Kind.BLOCK)) {
      SyntaxToken previousToken = block.firstToken();
      int previousColumn = previousToken.column();
      int previousLine = previousToken.line();
      SyntaxToken currentToken = current.firstToken();
      int currentColumn = currentToken.column();
      int currentLine = currentToken.line();
      if ((previousColumn == currentColumn && previousLine + 1 == currentLine) ||
        (previousLine == previous.firstToken().line()
          && previous.firstToken().column() < currentColumn)) {
        int lines = 1 + currentLine - previousLine;
        context.reportIssue(this, current, getMessage(condition, lines));
      }
    }
  }

  private static String getMessage(boolean ifStatementMessage, int lines) {
    if (lines == 1) {
      return ifStatementMessage ? IF_MESSAGE_ONE_LINER : LOOP_MESSAGE_ONE_LINER;
    }
    return String.format(Locale.US, ifStatementMessage ? IF_MESSAGE : LOOP_MESSAGE, lines);
  }

  private static StatementTree getIfStatementLastBlock(StatementTree statementTree) {
    StatementTree block = statementTree;
    while (block.is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatementTree = (IfStatementTree) block;
      StatementTree elseStatement = ifStatementTree.elseStatement();
      block = elseStatement == null ? ifStatementTree.thenStatement() : elseStatement;
    }
    return block;
  }
}
