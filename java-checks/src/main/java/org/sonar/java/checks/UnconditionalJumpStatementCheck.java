/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Rule(key = "S1751")
public class UnconditionalJumpStatementCheck extends IssuableSubscriptionVisitor {

  private static final Tree.Kind[] LOOP_KINDS = {
    Tree.Kind.DO_STATEMENT,
    Tree.Kind.WHILE_STATEMENT,
    Tree.Kind.FOR_STATEMENT,
    Tree.Kind.FOR_EACH_STATEMENT
  };

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.BREAK_STATEMENT,
      Tree.Kind.CONTINUE_STATEMENT,
      Tree.Kind.RETURN_STATEMENT,
      Tree.Kind.THROW_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    Tree parent = tree.parent();
    while (parent.is(Tree.Kind.BLOCK)) {
      parent = parent.parent();
    }

    if (!parent.is(LOOP_KINDS)) {
      return;
    }

    if (tree.is(Tree.Kind.CONTINUE_STATEMENT) || !canExecuteMoreThanOnce(parent)) {
      SyntaxToken jumpKeyword = jumpKeyword(tree);
      reportIssue(jumpKeyword, String.format("Remove this \"%s\" statement or make it conditional.", jumpKeyword.text()));
    }
  }

  private static boolean canExecuteMoreThanOnce(Tree loopTree) {
    CFG cfg = getCFG(loopTree);
    return cfg.blocks().stream().anyMatch(block -> loopTree.equals(block.terminator()) && hasPredecessorInLoopBody(block.predecessors(), loopTree));
  }

  private static boolean hasPredecessorInLoopBody(Set<Block> predecessors, Tree loop) {
    for (CFG.Block predecessor : predecessors) {
      List<Tree> predecessorElements = predecessor.elements();
      if (!predecessorElements.isEmpty()) {
        Tree predecessorLastElement = predecessorElements.get(predecessorElements.size() - 1);

        if (isForStatementInitializer(predecessorLastElement, loop)) {
          continue;
        }

        if (isForStatementUpdate(predecessorLastElement, loop)) {
          // there is a way to reach the update
          return !predecessor.predecessors().isEmpty();
        }

        if (isDescendant(predecessorLastElement, loop)) {
          return true;
        }
      } else {
        return hasPredecessorInLoopBody(predecessor.predecessors(), loop);
      }
    }
    return false;
  }

  private static boolean isForStatementInitializer(Tree lastElement, Tree loop) {
    if (loop.is(Tree.Kind.FOR_STATEMENT)) {
      ListTree<StatementTree> initializer = ((ForStatementTree) loop).initializer();
      if (!initializer.isEmpty()) {
        return isDescendant(lastElement, initializer);
      }
    } else if (loop.is(Tree.Kind.FOR_EACH_STATEMENT)) {
      return isDescendant(lastElement, ((ForEachStatement) loop).expression());
    }
    return false;
  }

  private static boolean isForStatementUpdate(Tree lastElement, Tree loop) {
    if (loop.is(Tree.Kind.FOR_STATEMENT)) {
      ListTree<StatementTree> update = ((ForStatementTree) loop).update();
      if (!update.isEmpty()) {
        return isDescendant(lastElement, update);
      }
    }
    return false;
  }

  private static boolean isDescendant(Tree descendant, Tree target) {
    Tree parent = descendant;
    while (parent != null) {
      if (parent.equals(target)) {
        return true;
      }
      parent = parent.parent();
    }
    return false;
  }

  private static SyntaxToken jumpKeyword(Tree jumpStatement) {
    if (jumpStatement.is(Tree.Kind.BREAK_STATEMENT)) {
      return ((BreakStatementTree) jumpStatement).breakKeyword();
    } else if (jumpStatement.is(Tree.Kind.CONTINUE_STATEMENT)) {
      return ((ContinueStatementTree) jumpStatement).continueKeyword();
    } else if (jumpStatement.is(Tree.Kind.RETURN_STATEMENT)) {
      return ((ReturnStatementTree) jumpStatement).returnKeyword();
    } else {
      return ((ThrowStatementTree) jumpStatement).throwKeyword();
    }
  }

  private static CFG getCFG(Tree loop) {
    Tree currentTree = loop;
    do {
      currentTree = currentTree.parent();
    } while (!currentTree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR, Tree.Kind.INITIALIZER, Tree.Kind.STATIC_INITIALIZER));

    if (currentTree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
      return CFG.build((MethodTree) currentTree);
    }
    return CFG.buildCFG(((BlockTree) currentTree).body());
  }

}
