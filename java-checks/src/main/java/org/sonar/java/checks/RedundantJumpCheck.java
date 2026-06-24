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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.DoWhileStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

@Rule(key = "S3626")
public class RedundantJumpCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.METHOD,
      Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (methodTree.block() != null) {
      CFG cfg = (CFG) methodTree.cfg();
      cfg.blocks().forEach(this::checkBlock);
    }
  }

  private void checkBlock(Block block) {
    Block successorWithoutJump = block.successorWithoutJump();
    Tree terminator = block.terminator();

    if (terminator != null
      && successorWithoutJump != null
      && terminator.is(Tree.Kind.CONTINUE_STATEMENT, Tree.Kind.RETURN_STATEMENT)
      && !isReturnWithExpression(terminator)
      && !isSwitchCaseChild(terminator)) {

      successorWithoutJump = nonEmptySuccessor(successorWithoutJump);
      Iterator<Block> successors = block.successors().iterator();
      if (successors.hasNext()) {
        Block successor = nonEmptySuccessor(successors.next());
        if (successor.equals(successorWithoutJump)
          && !isJumpThroughFinallyWithDistinctContinuation(terminator, successor)) {
          reportIssue(terminator, "Remove this redundant jump.");
        }
      }
    }
  }

  private static boolean isReturnWithExpression(Tree tree) {
    if (tree.is(Tree.Kind.RETURN_STATEMENT)) {
      return ((ReturnStatementTree) tree).expression() != null;
    }
    return false;
  }

  private static boolean isSwitchCaseChild(Tree tree) {
    return tree.parent().is(Tree.Kind.CASE_GROUP);
  }

  private static Block nonEmptySuccessor(Block initialBlock) {
    Block result = initialBlock;
    Set<Integer> visited = new HashSet<>();
    while (result.elements().isEmpty() && result.successors().size() == 1 && visited.add(result.id())) {
      result = result.successors().iterator().next();
    }
    return result;
  }

  private static boolean isFinallyBlockWithDistinctContinuation(Block block) {
    Block exitBlock = block.exitBlock();
    return block.isFinallyBlock()
      && exitBlock != null
      && block.successors().stream()
        .anyMatch(s -> !s.equals(exitBlock) && !isDeadLoopExitingTo(s, exitBlock));
  }

  private static boolean isJumpThroughFinallyWithDistinctContinuation(Tree terminator, Block successor) {
    if (!isFinallyBlockWithDistinctContinuation(successor)) {
      return false;
    }
    if (terminator.is(Tree.Kind.RETURN_STATEMENT)) {
      return true;
    }
    return hasFollowingStatementAfterEnclosingTryFinallyBeforeLoopContinuation(terminator);
  }

  private static TryStatementTree enclosingTryFinally(Tree tree) {
    Tree current = tree.parent();
    while (!current.is(Tree.Kind.TRY_STATEMENT) || ((TryStatementTree) current).finallyBlock() == null) {
      current = current.parent();
    }
    return (TryStatementTree) current;
  }

  private static boolean hasFollowingStatementAfterEnclosingTryFinallyBeforeLoopContinuation(Tree tree) {
    return hasFollowingStatementBeforeLoopContinuation(enclosingTryFinally(tree));
  }

  private static boolean hasFollowingStatementBeforeLoopContinuation(Tree tree) {
    Tree current = tree;
    Tree parent = current.parent();
    while (!parent.is(Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT, Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT)) {
      if (parent.is(Tree.Kind.BLOCK) && hasFollowingStatement((BlockTree) parent, current)) {
        return true;
      }
      current = parent;
      parent = current.parent();
    }
    return false;
  }

  private static boolean hasFollowingStatement(BlockTree block, Tree statement) {
    List<StatementTree> statements = block.body();
    int statementIndex = statements.indexOf(statement);
    return statementIndex >= 0 && statementIndex < statements.size() - 1;
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
