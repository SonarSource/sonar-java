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
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import java.util.Arrays;
import java.util.List;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "S00121", repositoryKey = "squid")
@Rule(key = "S121")
public class MissingCurlyBracesCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.IF_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT, Tree.Kind.FOR_STATEMENT, Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case WHILE_STATEMENT:
        WhileStatementTree whileStatementTree = (WhileStatementTree) tree;
        checkStatement(whileStatementTree.whileKeyword(), whileStatementTree.statement());
        break;
      case DO_STATEMENT:
        DoWhileStatementTree doWhileStatementTree = (DoWhileStatementTree) tree;
        checkStatement(doWhileStatementTree.doKeyword(), doWhileStatementTree.statement());
        break;
      case FOR_STATEMENT:
        ForStatementTree forStatementTree = (ForStatementTree) tree;
        checkStatement(forStatementTree.forKeyword(), forStatementTree.statement());
        break;
      case FOR_EACH_STATEMENT:
        ForEachStatement forEachStatement = (ForEachStatement) tree;
        checkStatement(forEachStatement.forKeyword(), forEachStatement.statement());
        break;
      case IF_STATEMENT:
        checkIfStatement((IfStatementTree) tree);
        break;
      default:
        break;
    }
  }

  private void checkIfStatement(IfStatementTree ifStmt) {
    StatementTree thenStatement = ifStmt.thenStatement();
    StatementTree elseStmt = ifStmt.elseStatement();
    boolean haveElse = elseStmt != null;
    if (!isException(thenStatement, LineUtils.startLine(ifStmt.ifKeyword()), haveElse)) {
      checkStatement(ifStmt.ifKeyword(), thenStatement);
    }
    if (haveElse && !elseStmt.is(Tree.Kind.IF_STATEMENT)) {
      checkStatement(ifStmt.elseKeyword(), elseStmt);
    }
  }

  private static boolean isException(StatementTree statementTree, int ifLine, boolean haveElse) {
    if (haveElse || !statementTree.is(Tree.Kind.RETURN_STATEMENT, Tree.Kind.CONTINUE_STATEMENT, Tree.Kind.BREAK_STATEMENT)) {
      return false;
    }
    return LineUtils.startLine(statementTree) == ifLine;
  }

  private void checkStatement(SyntaxToken reportToken, StatementTree statement) {
    if (!statement.is(Tree.Kind.BLOCK)) {
      reportIssue(reportToken, "Missing curly brace.");
    }
  }
}
