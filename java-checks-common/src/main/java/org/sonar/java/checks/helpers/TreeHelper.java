/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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

  public static Tree findClosestParentOfKind(Tree tree, Set<Tree.Kind> nodeKinds) {
    while (tree != null) {
      if (nodeKinds.contains(tree.kind())) {
        return tree;
      }
      tree = tree.parent();
    }
    return null;
  }

  private static class ReachableMethodsFinder extends BaseTreeVisitor {
    private final Map<MethodTree, Void> reachableMethods = new IdentityHashMap<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      var declaration = tree.methodSymbol().declaration();
      if (declaration != null && declaration.is(Tree.Kind.METHOD) && !reachableMethods.containsKey(declaration)) {
        reachableMethods.put(declaration, null);
        declaration.accept(this);
      }
    }

    public Set<MethodTree> getReachableMethods() {
      return reachableMethods.keySet();
    }
  }
}
