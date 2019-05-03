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
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2062")
public class PrivateReadResolveCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree method = (MethodTree) tree;
    if (isPrivateReadResolve(method) && isOwnedBySerializableExtensibleClass(method)) {
      reportIssue(method.simpleName(), "Make this class \"private\" or elevate the visibility of \"readResolve\".");
    }
  }

  private static boolean isPrivateReadResolve(MethodTree method) {
    return "readResolve".equals(method.simpleName().name()) && method.parameters().isEmpty() && ModifiersUtils.hasModifier(method.modifiers(), Modifier.PRIVATE);
  }

  private static boolean isOwnedBySerializableExtensibleClass(MethodTree method) {
    Symbol owner = method.symbol().owner();
    return !owner.isPrivate() && !owner.isFinal() && owner.type().isSubtypeOf("java.io.Serializable");
  }
}
