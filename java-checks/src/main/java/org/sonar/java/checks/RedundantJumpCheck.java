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
import java.util.Iterator;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.cfg.CFGUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

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
      && !isSwitchCaseChild(terminator)
      && hasValidContinueTarget(terminator)) {

      successorWithoutJump = CFGUtils.nonEmptySuccessor(successorWithoutJump);
      Iterator<Block> successors = block.successors().iterator();
      if (successors.hasNext()) {
        Block successor = CFGUtils.nonEmptySuccessor(successors.next());
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

  private static boolean hasValidContinueTarget(Tree tree) {
    return !tree.is(Tree.Kind.CONTINUE_STATEMENT) || hasEnclosingLoop(tree);
  }

  private static boolean hasEnclosingLoop(Tree tree) {
    Tree current = tree.parent();
    while (current != null) {
      if (isLoop(current)) {
        return true;
      }
      current = current.parent();
    }
    return false;
  }

  private static boolean isLoop(Tree tree) {
    return tree.is(Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT, Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT);
  }

  private static boolean isJumpThroughFinallyWithDistinctContinuation(Tree terminator, Block successor) {
    if (!CFGUtils.isFinallyBlockWithDistinctContinuation(successor)) {
      return false;
    }
    if (terminator.is(Tree.Kind.RETURN_STATEMENT)) {
      return true;
    }
    return hasFollowingStatementAfterEnclosingTryFinallyBeforeLoopContinuation(terminator);
  }

  private static TryStatementTree enclosingTryFinally(Tree tree) {
    Tree current = tree.parent();
    while (current != null
      && (!current.is(Tree.Kind.TRY_STATEMENT) || ((TryStatementTree) current).finallyBlock() == null)) {
      current = current.parent();
    }
    return (TryStatementTree) current;
  }

  private static boolean hasFollowingStatementAfterEnclosingTryFinallyBeforeLoopContinuation(Tree tree) {
    TryStatementTree tryStatement = enclosingTryFinally(tree);
    return tryStatement == null || hasFollowingStatementBeforeLoopContinuation(tryStatement);
  }

  private static boolean hasFollowingStatementBeforeLoopContinuation(Tree tree) {
    Tree current = tree;
    Tree parent = current.parent();
    while (parent != null
      && !isLoop(parent)) {
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

}
