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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S00121",
  name = "Control structures should use curly braces",
  tags = {"cert", "cwe", "misra", "pitfall"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class MissingCurlyBraces_S00121_Check extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.IF_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT, Tree.Kind.FOR_STATEMENT, Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case WHILE_STATEMENT:
        checkStatement(((WhileStatementTree) tree).statement(), tree);
        break;
      case DO_STATEMENT:
        checkStatement(((DoWhileStatementTree) tree).statement(), tree);
        break;
      case FOR_STATEMENT:
        checkStatement(((ForStatementTree) tree).statement(), tree);
        break;
      case FOR_EACH_STATEMENT:
        checkStatement(((ForEachStatement) tree).statement(), tree);
        break;
      case IF_STATEMENT:
        IfStatementTree ifStmt = (IfStatementTree) tree;
        checkIfStatement(ifStmt);
        break;
      default:
        break;
    }
  }

  private void checkIfStatement(IfStatementTree ifStmt) {
    checkStatement(ifStmt.thenStatement(), ifStmt);
    StatementTree elseStmt = ifStmt.elseStatement();
    if (elseStmt != null && !elseStmt.is(Tree.Kind.IF_STATEMENT)) {
      checkStatement(elseStmt, ifStmt.elseKeyword());
    }
  }

  private void checkStatement(StatementTree statement, Tree tree) {
    if (!statement.is(Tree.Kind.BLOCK)) {
      addIssue(tree, "Missing curly brace.");
    }
  }
}
