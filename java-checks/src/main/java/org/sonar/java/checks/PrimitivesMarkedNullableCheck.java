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
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S4682")
public final class PrimitivesMarkedNullableCheck extends IssuableSubscriptionVisitor {

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
    TypeTree returnType = methodTree.returnType();
    if (returnType.symbolType().isPrimitive()) {
      findNullableAnnotation(methodTree)
        .ifPresent(annotation -> reportIssue(returnType, String.format("\"%s\" annotation should not be used on primitive types", annotation)));
    }
  }

  private static Optional<String> findNullableAnnotation(MethodTree methodTree) {
    SymbolMetadata methodMetadata = methodTree.symbol().metadata();
    if (methodMetadata.isAnnotatedWith("javax.annotation.CheckForNull")) {
      return Optional.of("@CheckForNull");
    } else if (methodMetadata.isAnnotatedWith("javax.annotation.Nullable")) {
      return Optional.of("@Nullable");
    }
    return Optional.empty();
  }

}
