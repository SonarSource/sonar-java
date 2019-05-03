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
package org.sonar.java.checks.serialization;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Rule(key = "S2060")
public class ExternalizableClassConstructorCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    if (!isAnonymous(classTree) && implementsExternalizable(classTree)) {
      Collection<Symbol> constructors = classTree.symbol().lookupSymbols("<init>");
      boolean hasNoArgConstructor = constructors.isEmpty();
      for (Symbol constructor : constructors) {
        if (isNoArgConstructor(constructor)) {
          hasNoArgConstructor = true;
          break;
        }
      }
      if (!hasNoArgConstructor) {
        reportIssue(classTree.simpleName(), "Add a no-arg constructor to this class.");
      }
    }
  }

  private static boolean isAnonymous(ClassTree classTree) {
    return classTree.simpleName() == null;
  }

  private static boolean implementsExternalizable(ClassTree classTree) {
    return classTree.symbol().type().isSubtypeOf("java.io.Externalizable");
  }

  private static boolean isNoArgConstructor(Symbol constructor) {
    return ((Symbol.MethodSymbol) constructor).parameterTypes().isEmpty();
  }
}
