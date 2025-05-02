/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "S00108", repositoryKey = "squid")
@Rule(key = "S108")
public class EmptyBlockCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove this block of code, fill it in, or add a comment explaining why it is empty.";
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
