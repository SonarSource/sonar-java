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

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2186")
public class AssertionInThreadRunCheck extends IssuableSubscriptionVisitor {

  private static final Iterable<String> CHECKED_TYPES = ImmutableList.of(
    "org.junit.Assert",
    "org.junit.jupiter.api.Assertions",
    "junit.framework.Assert",
    "junit.framework.TestCase",
    "org.fest.assertions.Assertions");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    BlockTree block = methodTree.block();
    if (block != null && isRunMethod(methodTree)) {
      block.accept(new AssertionsVisitor());
    }
  }

  private static boolean isRunMethod(MethodTree methodTree) {
    return methodTree.symbol().owner().type().isSubtypeOf("java.lang.Runnable") && "run".equals(methodTree.simpleName().name()) && methodTree.parameters().isEmpty();
  }

  private class AssertionsVisitor extends BaseTreeVisitor {
    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if(tree.symbol().isMethodSymbol()) {
        Type type = tree.symbol().owner().type();
        if (isCheckedType(type)) {
          reportIssue(ExpressionUtils.methodName(tree), "Remove this assertion.");
        }
      }
      super.visitMethodInvocation(tree);
    }

    private boolean isCheckedType(Type type) {
      for (String checkedType : CHECKED_TYPES) {
        if (type.is(checkedType)) {
          return true;
        }
      }
      return false;
    }
  }
}
