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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S2176")
public class InterfaceOrSuperclassShadowingCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (hasSemantic()) {
      Symbol.TypeSymbol classSymbol = classTree.symbol();
      checkSuperType(classTree, classSymbol.superClass());
      for (Type interfaceType : classSymbol.interfaces()) {
        checkSuperType(classTree, interfaceType);
      }
    }
  }

  private void checkSuperType(ClassTree tree, @Nullable Type superType) {
    if (superType != null && superType.symbol().name().equals(tree.symbol().name())) {
      String classOrInterface = tree.is(Tree.Kind.CLASS) ? "class" : "interface";
      reportIssue(tree.simpleName(), "Rename this " + classOrInterface + ".");
    }
  }

}
