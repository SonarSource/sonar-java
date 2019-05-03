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
import org.sonar.check.Rule;
import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S3551")
public class SynchronizedOverrideCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make this method \"synchronized\" to match the parent class implementation.";

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
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();
    Symbol.MethodSymbol overriddenSymbol =  methodSymbol.overriddenSymbol();
    if (overriddenSymbol == null || overriddenSymbol.isUnknown()) {
      return;
    }
    if (isSynchronized(overriddenSymbol) && !isSynchronized(methodSymbol)) {
      List<JavaFileScannerContext.Location> secondaries = Collections.emptyList();
      MethodTree overridenMethodTree = overriddenSymbol.declaration();
      if (overridenMethodTree != null) {
        secondaries = Collections.singletonList(new JavaFileScannerContext.Location("", overridenMethodTree.simpleName()));
      }
      reportIssue(methodTree.simpleName(), MESSAGE, secondaries, null);
    }
  }

  private static boolean isSynchronized(Symbol methodSymbol) {
    return Flags.isFlagged(
      ((JavaSymbol) methodSymbol).flags(),
      Flags.SYNCHRONIZED);
  }

}
