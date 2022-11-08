/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "RightCurlyBraceStartLineCheck", repositoryKey = "squid")
@Rule(key = "S1109")
public class RightCurlyBraceStartLineCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return List.of(
      Tree.Kind.BLOCK,
      Tree.Kind.STATIC_INITIALIZER,
      Tree.Kind.INITIALIZER,
      Tree.Kind.CLASS,
      Tree.Kind.INTERFACE,
      Tree.Kind.ANNOTATION_TYPE,
      Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.BLOCK, Tree.Kind.STATIC_INITIALIZER, Tree.Kind.INITIALIZER)) {
      BlockTree blockTree = (BlockTree) tree;
      checkBlockBody(blockTree.openBraceToken(), blockTree.closeBraceToken(), blockTree.body());
    } else {
      ClassTree classTree = (ClassTree) tree;
      checkBlockBody(classTree.openBraceToken(), classTree.closeBraceToken(), classTree.members());
    }
  }

  private void checkBlockBody(SyntaxToken openBraceToken, SyntaxToken closeBraceToken, List<? extends Tree> trees) {
    if (openBraceToken.range().start().line() != closeBraceToken.range().start().line() && !trees.isEmpty()) {
      Tree lastTree = trees.get(trees.size() - 1);
      if (lastTree.lastToken().range().start().line() == closeBraceToken.range().start().line()) {
        reportIssue(closeBraceToken, "Move this closing curly brace to the next line.");
      }
    }
  }
}
