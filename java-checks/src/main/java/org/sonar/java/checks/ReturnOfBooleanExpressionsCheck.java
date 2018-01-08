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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import javax.annotation.Nullable;

import java.util.List;

@Rule(key = "S1126")
public class ReturnOfBooleanExpressionsCheck extends IssuableSubscriptionVisitor {


  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.IF_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    IfStatementTree ifStatementTree = (IfStatementTree) tree;
    StatementTree elseStatementOrNextStatement = getStatementTree(ifStatementTree);
    if (hasOneReturnBoolean(elseStatementOrNextStatement) && hasOneReturnBoolean(ifStatementTree.thenStatement())) {
      reportIssue(ifStatementTree.ifKeyword(), "Replace this if-then-else statement by a single return statement.");
    }
  }

  private static StatementTree getStatementTree(IfStatementTree ifStatementTree) {
    StatementTree elseStatementOrNextStatement = ifStatementTree.elseStatement();
    if (elseStatementOrNextStatement == null) {
      JavaTree parent = (JavaTree) ifStatementTree.parent();
      List<Tree> children = parent.getChildren();
      int indexOfIf = children.indexOf(ifStatementTree);
      if (indexOfIf < children.size() - 1) {
        // Defensive, this condition should always be true as if necessarily followed by a statement or a token.
        Tree next = children.get(indexOfIf + 1);
        if(!next.is(Kind.TOKEN)) {
          elseStatementOrNextStatement = (StatementTree) next;
        }
      }
    }
    return elseStatementOrNextStatement;
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
    if (statementTree.is(Kind.RETURN_STATEMENT)) {
      ReturnStatementTree returnStatement = (ReturnStatementTree) statementTree;
      return returnStatement.expression() != null && returnStatement.expression().is(Kind.BOOLEAN_LITERAL);
    }
    return false;
  }
}
