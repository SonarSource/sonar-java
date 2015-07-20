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
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
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
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Locale;

@Rule(
  key = "S2681",
  name = "Multiline blocks should be enclosed in curly braces",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
public class MultilineBlocksCurlyBracesCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String LOOP_MESSAGE = "Only the first line of this %d-line block will be executed in a loop. The rest will execute only once.";
  private static final String IF_MESSAGE = "Only the first line of this %d-line block will be executed conditionally. The rest will execute unconditionally.";
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
      SyntaxToken previousToken = FirstSyntaxTokenFinder.firstSyntaxToken(block);
      int previousColumn = previousToken.column();
      int previousLine = previousToken.line();
      SyntaxToken currentToken = FirstSyntaxTokenFinder.firstSyntaxToken(current);
      int currentColumn = currentToken.column();
      int currentLine = currentToken.line();
      if (previousColumn == currentColumn) {
        context.addIssue(current, this, String.format(Locale.US, condition ? IF_MESSAGE : LOOP_MESSAGE, 1 + currentLine - previousLine));
      }
    }
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
