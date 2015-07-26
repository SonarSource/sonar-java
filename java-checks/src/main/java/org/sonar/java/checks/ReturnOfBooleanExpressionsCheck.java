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
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;
import java.util.List;

@Rule(
  key = "S1126",
  name = "Return of boolean expressions should not be wrapped into an \"if-then-else\" statement",
  tags = {"clumsy"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class ReturnOfBooleanExpressionsCheck extends SubscriptionBaseVisitor {


  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    IfStatementTree ifStatementTree = (IfStatementTree) tree;
    if (hasOneReturnBoolean(ifStatementTree.elseStatement()) && hasOneReturnBoolean(ifStatementTree.thenStatement())) {
      addIssue(tree, "Replace this if-then-else statement by a single return statement.");
    }
  }

  private static boolean hasOneReturnBoolean(@Nullable StatementTree statementTree) {
    if (statementTree == null) {
      return false;
    }
    if (statementTree.is(Kind.BLOCK)) {
      BlockTree block = (BlockTree) statementTree;
      return block.body().size() == 1 && isReturnBooleanLiteral(block.body().get(0));
    }
    return isReturnBooleanLiteral(statementTree);
  }

  private static boolean isReturnBooleanLiteral(StatementTree statementTree) {
    if(statementTree.is(Kind.RETURN_STATEMENT)) {
      ReturnStatementTree returnStatement = (ReturnStatementTree) statementTree;
      return returnStatement.expression() != null && returnStatement.expression().is(Kind.BOOLEAN_LITERAL);
    }
    return false;
  }
}
