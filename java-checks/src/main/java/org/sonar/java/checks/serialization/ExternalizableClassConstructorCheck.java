/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2060")
public class ExternalizableClassConstructorCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (!isAnonymous(classTree) && implementsExternalizable(classTree)) {
      Collection<Symbol> constructors = classTree.symbol().lookupSymbols("<init>");
      var noArgConstructor = constructors.stream().filter(ExternalizableClassConstructorCheck::isNoArgConstructor).findFirst();

      if (noArgConstructor.isEmpty()) {
        reportIssue(Objects.requireNonNull(classTree.simpleName()), "Add a no-arg constructor to this class.");
      } else if (!noArgConstructor.get().isPublic()) {
        // Implicit no-arg constructors have no declaration and same visibility as class. Can be below "public". Ignore them.
        var declaration = noArgConstructor.get().declaration();
        if (declaration != null) {
          reportIssue(((MethodTree) declaration).simpleName(), "Declare this no-arg constructor public.");
        }
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
