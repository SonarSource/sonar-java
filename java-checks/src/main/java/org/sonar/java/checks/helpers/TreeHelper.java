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
package org.sonar.java.checks.helpers;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

public class TreeHelper {
  private TreeHelper() {
    // Prevent instantiation
  }

  /**
   * Will find all methods that can be reached from this tree because they may be called (transitively) by the current method or callees.
   */
  public static Set<MethodTree> findReachableMethodsInSameFile(Tree tree) {
    var finder = new ReachableMethodsFinder();
    tree.accept(finder);
    return finder.getReachableMethods();
  }

  private static class ReachableMethodsFinder extends BaseTreeVisitor {
    private Map<MethodTree, Void> reachableMethods = new IdentityHashMap<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      var declaration = tree.methodSymbol().declaration();
      if (declaration != null && declaration.is(Tree.Kind.METHOD) && !reachableMethods.containsKey(declaration)) {
        reachableMethods.put((MethodTree) declaration, null);
        declaration.accept(this);
      }
    }

    public Set<MethodTree> getReachableMethods() {
      return reachableMethods.keySet();
    }
  }
}
