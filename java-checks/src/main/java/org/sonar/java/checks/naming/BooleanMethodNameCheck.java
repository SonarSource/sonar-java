/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.checks.naming;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type.Primitives;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2047")
public class BooleanMethodNameCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    IdentifierTree simpleName = methodTree.simpleName();
    if (returnsBoolean(methodTree) && isBooleanGetter(simpleName) && isNotOverriding(methodTree)) {
      reportIssue(simpleName, "Rename this method to start with \"is\" or \"has\".");
    }
  }

  private static boolean isBooleanGetter(IdentifierTree simpleName) {
    String text = simpleName.identifierToken().text();
    return text.startsWith("get") && !text.startsWith("getBoolean");
  }

  private static boolean isNotOverriding(MethodTree methodTree) {
    return Boolean.FALSE.equals(methodTree.isOverriding());
  }

  private static boolean returnsBoolean(MethodTree methodTree) {
    return methodTree.returnType().symbolType().isPrimitive(Primitives.BOOLEAN);
  }
}
