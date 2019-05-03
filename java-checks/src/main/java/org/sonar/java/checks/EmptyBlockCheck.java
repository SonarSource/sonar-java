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

import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S00108")
@RspecKey("S108")
public class EmptyBlockCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Either remove or fill this block of code.";
  private boolean isMethodBlock;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.METHOD,
      Tree.Kind.CONSTRUCTOR,
      Tree.Kind.BLOCK,
      Tree.Kind.INITIALIZER,
      Tree.Kind.STATIC_INITIALIZER,
      Tree.Kind.SWITCH_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.SWITCH_STATEMENT)) {
      SwitchStatementTree switchStatementTree = (SwitchStatementTree) tree;
      if (switchStatementTree.cases().isEmpty()) {
        reportIssue(switchStatementTree.openBraceToken(), MESSAGE);
      }
    } else if (tree.is(Tree.Kind.METHOD) || tree.is(Tree.Kind.CONSTRUCTOR)) {
      isMethodBlock = true;
    } else {
      if (isMethodBlock) {
        isMethodBlock = false;
      } else if (!tree.parent().is(Tree.Kind.LAMBDA_EXPRESSION)
              && !hasStatements((BlockTree) tree)
              && !isRuleException((BlockTree) tree)) {
        reportIssue(((BlockTree) tree).openBraceToken(), MESSAGE);
      }
    }
  }

  private static boolean isRuleException(BlockTree tree) {
    return hasCommentInside(tree) && !tree.parent().is(Tree.Kind.SYNCHRONIZED_STATEMENT);
  }

  private static boolean hasCommentInside(BlockTree tree) {
    return tree.closeBraceToken() == null || !tree.closeBraceToken().trivias().isEmpty();
  }

  private static boolean hasStatements(BlockTree tree) {
    return !tree.body().isEmpty();
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD) || tree.is(Tree.Kind.CONSTRUCTOR)) {
      isMethodBlock = false;
    }
  }

}
