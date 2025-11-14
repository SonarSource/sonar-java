/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S1199")
public class NestedBlocksCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitCaseGroup(CaseGroupTree tree) {
    List<StatementTree> body = tree.body();
    int statementsInBody = body.size();
    if (statementsInBody == 0) {
      return;
    }

    if (isTerminalStatement(body.get(statementsInBody - 1))) {
      // If the last statement is a break, yield or return, we do not count it
      statementsInBody--;
    }

    if (statementsInBody > 1) {
      checkStatements(tree.body());
    }

    super.visitCaseGroup(tree);
  }

  @Override
  public void visitBlock(BlockTree tree) {
    checkStatements(tree.body());
    super.visitBlock(tree);
  }

  private static boolean isTerminalStatement(StatementTree statementTree) {
    return statementTree.is(Tree.Kind.YIELD_STATEMENT, Tree.Kind.BREAK_STATEMENT, Tree.Kind.RETURN_STATEMENT);
  }

  private void checkStatements(List<StatementTree> statements) {
    for (StatementTree statement : statements) {
      if (statement.is(Tree.Kind.BLOCK)) {
        context.reportIssue(this, ((BlockTree) statement).openBraceToken(), "Extract this nested code block into a method.");
      }
    }
  }

}
