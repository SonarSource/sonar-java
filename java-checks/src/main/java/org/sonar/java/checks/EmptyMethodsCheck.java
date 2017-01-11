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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(key = "S1186")
public class EmptyMethodsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT)) {
      for (Tree member : classTree.members()) {
        if (member.is(Kind.METHOD) || isPublicNoArgConstructor(member)) {
          checkMethod((MethodTree) member);
        }
      }
    }
  }

  private static boolean isPublicNoArgConstructor(Tree node) {
    return node.is(Kind.CONSTRUCTOR) && ModifiersUtils.hasModifier(((MethodTree) node).modifiers(), Modifier.PUBLIC) && ((MethodTree) node).parameters().isEmpty();
  }

  private void checkMethod(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    if (block != null && isEmpty(block) && !containsComment(block)) {
      reportIssue(methodTree.simpleName(), "Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or complete the implementation.");
    }
  }

  private static boolean isEmpty(BlockTree block) {
    List<StatementTree> body = block.body();
    return body.isEmpty() || body.stream().allMatch(stmt -> stmt.is(Kind.EMPTY_STATEMENT));
  }

  private static boolean containsComment(BlockTree block) {
    return !block.closeBraceToken().trivias().isEmpty();
  }

}
