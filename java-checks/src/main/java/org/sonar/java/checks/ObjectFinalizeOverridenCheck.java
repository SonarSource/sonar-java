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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "ObjectFinalizeOverridenCheck")
@RspecKey("S1113")
public class ObjectFinalizeOverridenCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (isFinalize(methodTree)) {
      reportIssue(methodTree.simpleName(), "Do not override the Object.finalize() method.");
    }
  }

  private static boolean isFinalize(MethodTree methodTree) {
    if ("finalize".equals(methodTree.simpleName().name())) {
      Tree returnType = methodTree.returnType();
      if (returnType != null && returnType.is(Tree.Kind.PRIMITIVE_TYPE)) {
        return "void".equals(((PrimitiveTypeTree) returnType).keyword().text());
      }
    }
    return false;
  }

}
