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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WildcardTree;

@Rule(key = "S1452")
public class WildcardReturnParameterTypeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (!isPrivate(methodTree) && !isOverriding(methodTree)) {
      methodTree.returnType().accept(new CheckWildcard());
    }
  }

  private static boolean isPrivate(MethodTree methodTree) {
    return ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.PRIVATE);
  }

  private static boolean isOverriding(MethodTree tree) {
    return Boolean.TRUE.equals(tree.isOverriding());
  }

  private class CheckWildcard extends BaseTreeVisitor {

    private boolean classType = false;

    @Override
    public void visitParameterizedType(ParameterizedTypeTree tree) {
      classType = tree.type().symbolType().is("java.lang.Class");
      super.visitParameterizedType(tree);
      classType = false;
    }

    @Override
    public void visitWildcard(WildcardTree tree) {
      if (!classType) {
        reportIssue(tree.queryToken(), "Remove usage of generic wildcard type.");
      }
    }
  }

}
