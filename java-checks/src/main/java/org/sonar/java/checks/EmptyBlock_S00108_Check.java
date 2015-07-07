/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S00108",
  name = "Nested blocks of code should not be left empty",
  tags = {"bug"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
public class EmptyBlock_S00108_Check extends SubscriptionBaseVisitor {


  private boolean isMethodBlock;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR,
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
        addIssue(switchStatementTree, "Either remove or fill this block of code.");
      }
    } else if (tree.is(Tree.Kind.METHOD) || tree.is(Tree.Kind.CONSTRUCTOR)) {
      isMethodBlock = true;
    } else {
      if (isMethodBlock) {
        isMethodBlock = false;
      } else if (!hasStatements((BlockTree) tree) && !hasCommentInside((BlockTree) tree)) {
        addIssue(tree, "Either remove or fill this block of code.");
      }
    }
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
