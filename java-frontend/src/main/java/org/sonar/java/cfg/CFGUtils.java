/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.cfg;

import java.util.HashSet;
import java.util.Set;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

public final class CFGUtils {

  private CFGUtils() {
    // utility class
  }

  /**
   * Returns the first non-empty block reachable from the initial block by traversing single-successor empty blocks.
   */
  public static Block nonEmptySuccessor(Block initialBlock) {
    Block result = initialBlock;
    Set<Integer> visited = new HashSet<>();
    while (isEffectivelyEmpty(result) && result.successors().size() == 1 && visited.add(result.id())) {
      result = result.successors().iterator().next();
    }
    return result;
  }

  /**
   * Returns whether the block is a finally block whose normal continuation can differ from its jump exit continuation.
   */
  public static boolean isFinallyBlockWithDistinctContinuation(Block block) {
    Block exitBlock = block.exitBlock();
    if (!block.isFinallyBlock() || exitBlock == null) {
      return false;
    }
    Block successorAfterJump = nonEmptySuccessor(exitBlock);
    return block.successors().stream()
      .filter(successor -> !isDeadLoopExitingTo(successor, exitBlock))
      .map(CFGUtils::nonEmptySuccessor)
      .anyMatch(successor -> !successor.equals(successorAfterJump));
  }

  private static boolean isEffectivelyEmpty(Block block) {
    return block.elements().stream().allMatch(element -> element.is(Tree.Kind.EMPTY_STATEMENT));
  }

  private static boolean isDeadLoopExitingTo(Block successor, Block exitBlock) {
    Tree terminator = successor.terminator();
    if (terminator == null || !exitBlock.equals(successor.falseBlock())) {
      return false;
    }
    ExpressionTree condition = null;
    if (terminator.is(Tree.Kind.DO_STATEMENT)) {
      condition = ((DoWhileStatementTree) terminator).condition();
    } else if (terminator.is(Tree.Kind.FOR_STATEMENT)) {
      condition = ((ForStatementTree) terminator).condition();
    } else if (terminator.is(Tree.Kind.WHILE_STATEMENT)) {
      condition = ((WhileStatementTree) terminator).condition();
    }
    return condition != null && Boolean.FALSE.equals(ExpressionUtils.resolveAsConstant(condition));
  }
}
