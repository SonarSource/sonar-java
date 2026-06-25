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
import java.util.List;
import java.util.Set;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
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

  /**
   * Returns whether a jump reaching the provided successor through a finally block has a continuation that differs
   * from the fall-through path after the finally block.
   */
  public static boolean isJumpThroughFinallyWithDistinctContinuation(Tree terminator, Block successor) {
    if (!isFinallyBlockWithDistinctContinuation(successor)) {
      return false;
    }
    if (terminator.is(Tree.Kind.RETURN_STATEMENT)) {
      return true;
    }
    return hasFollowingStatementAfterEnclosingTryFinallyBeforeLoopContinuation(terminator);
  }

  private static boolean isEffectivelyEmpty(Block block) {
    return block.elements().stream().allMatch(element -> element.is(Tree.Kind.EMPTY_STATEMENT));
  }

  private static boolean hasFollowingStatementAfterEnclosingTryFinallyBeforeLoopContinuation(Tree tree) {
    TryStatementTree tryStatement = enclosingTryFinally(tree);
    return tryStatement == null || hasFollowingStatementBeforeLoopContinuation(tryStatement);
  }

  private static TryStatementTree enclosingTryFinally(Tree tree) {
    Tree current = tree.parent();
    while (current != null
      && (!current.is(Tree.Kind.TRY_STATEMENT) || ((TryStatementTree) current).finallyBlock() == null)) {
      current = current.parent();
    }
    return (TryStatementTree) current;
  }

  private static boolean hasFollowingStatementBeforeLoopContinuation(Tree tree) {
    Tree current = tree;
    Tree parent = current.parent();
    while (parent != null && !isLoop(parent)) {
      if (parent.is(Tree.Kind.BLOCK) && hasNonEmptyFollowingStatement((BlockTree) parent, current)) {
        return true;
      }
      current = parent;
      parent = current.parent();
    }
    return false;
  }

  private static boolean hasNonEmptyFollowingStatement(BlockTree block, Tree statement) {
    List<StatementTree> statements = block.body();
    int statementIndex = statements.indexOf(statement);
    return statementIndex >= 0
      && statements.subList(statementIndex + 1, statements.size()).stream()
        .anyMatch(s -> !s.is(Tree.Kind.EMPTY_STATEMENT));
  }

  private static boolean isLoop(Tree tree) {
    return tree.is(Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT, Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT);
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
