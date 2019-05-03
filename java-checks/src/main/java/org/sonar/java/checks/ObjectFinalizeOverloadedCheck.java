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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S1175")
public class ObjectFinalizeOverloadedCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (isFinalizeOverload(methodTree)) {
      reportIssue(methodTree.simpleName(), "Rename this method to avoid any possible confusion with Object.finalize().");
    }
  }

  private static boolean isFinalizeOverload(MethodTree methodTree) {
    return isNamedFinalize(methodTree) && !(hasNoParameter(methodTree) && isVoid(methodTree));
  }

  private static boolean isNamedFinalize(MethodTree methodTree) {
    return "finalize".equals(methodTree.simpleName().name());
  }

  private static boolean hasNoParameter(MethodTree methodTree) {
    return methodTree.parameters().isEmpty();
  }

  private static boolean isVoid(MethodTree methodTree) {
    TypeTree typeTree = methodTree.returnType();
    return typeTree.is(Tree.Kind.PRIMITIVE_TYPE) && "void".equals(((PrimitiveTypeTree) typeTree).keyword().text());
  }
}
