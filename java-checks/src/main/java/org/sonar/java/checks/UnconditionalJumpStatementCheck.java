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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ContinueStatementTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

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

    if (tree.is(Tree.Kind.CONTINUE_STATEMENT) || executeUnconditionnally(parent)) {
      SyntaxToken jumpKeyword = jumpKeyword(tree);
      reportIssue(jumpKeyword, String.format("Remove this \"%s\" statement or make it conditional.", jumpKeyword.text()));
    }
  }

  private static boolean executeUnconditionnally(Tree loopTree) {
    CFG cfg = getCFG(loopTree);
    CFG.Block loopBlock = getLoopBlock(cfg, loopTree);
    // we cannot find a path in the CFG that goes twice through this instruction.
    return !hasPredecessorInBlock(loopBlock, loopTree);
  }

  private static CFG.Block getLoopBlock(CFG cfg, Tree loopTree) {
    return cfg.blocks().stream()
      .filter(block -> loopTree.equals(block.terminator()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("CFG necessarily contains the loop block."));
  }

  private static boolean hasPredecessorInBlock(CFG.Block block, Tree loop) {
    for (CFG.Block predecessor : block.predecessors()) {
      List<Tree> predecessorElements = predecessor.elements();
      if (predecessorElements.isEmpty()) {
        return hasPredecessorInBlock(predecessor, loop);
      } else {
        Tree predecessorFirstElement = predecessorElements.get(0);

        if (isForStatementInitializer(predecessorFirstElement, loop)) {
          // skip 'for' loops initializers
          continue;
        }

        if (isForStatementUpdate(predecessorFirstElement, loop)) {
          // there is no way to reach the 'for' loop update
          return !predecessor.predecessors().isEmpty();
        }

        if (isDescendant(predecessorFirstElement, loop)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isForStatementInitializer(Tree lastElement, Tree loop) {
    if (loop.is(Tree.Kind.FOR_STATEMENT)) {
      return isDescendant(lastElement, ((ForStatementTree) loop).initializer());
    } else if (loop.is(Tree.Kind.FOR_EACH_STATEMENT)) {
      return isDescendant(lastElement, ((ForEachStatement) loop).expression());
    }
    return false;
  }

  private static boolean isForStatementUpdate(Tree lastElement, Tree loop) {
    if (loop.is(Tree.Kind.FOR_STATEMENT)) {
      return isDescendant(lastElement, ((ForStatementTree) loop).update());
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
