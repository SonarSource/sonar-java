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

import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import java.util.Arrays;
import java.util.List;

public abstract class RightCurlyBraceToNextBlockAbstractVisitor extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.IF_STATEMENT, Tree.Kind.TRY_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.IF_STATEMENT)) {
      IfStatementTree ifStatementTree = (IfStatementTree) tree;
      StatementTree thenStatement = ifStatementTree.thenStatement();
      if (ifStatementTree.elseKeyword() != null && thenStatement.is(Tree.Kind.BLOCK)) {
        checkTokenPosition(ifStatementTree.elseKeyword(), (BlockTree) thenStatement);
      }
    } else {
      TryStatementTree tryStatementTree = (TryStatementTree) tree;
      BlockTree block = tryStatementTree.block();
      for (CatchTree catchTree : tryStatementTree.catches()) {
        checkTokenPosition(catchTree.catchKeyword(), block);
        block = catchTree.block();
      }
      SyntaxToken finallyKeyword = tryStatementTree.finallyKeyword();
      if (finallyKeyword != null) {
        checkTokenPosition(finallyKeyword, block);
      }
    }
  }

  protected abstract void checkTokenPosition(SyntaxToken syntaxToken, BlockTree nextBlock);

}
