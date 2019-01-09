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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

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
      CFG cfg = CFG.build(methodTree);
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
      Block successor = nonEmptySuccessor(block.successors().iterator().next());
      if (successorWithoutJump.equals(successor)) {
        reportIssue(terminator, "Remove this redundant jump.");
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
    while (result.elements().isEmpty() && result.successors().size() == 1) {
      result = result.successors().iterator().next();
    }
    return result;
  }
}
