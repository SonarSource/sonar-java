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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

@Rule(key = "S4968")
public class TypeUpperBoundNotFinalCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.TYPE_PARAMETER, Tree.Kind.EXTENDS_WILDCARD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.TYPE_PARAMETER)) {
      handleBounds(((TypeParameterTree) tree).bounds(), tree);
    } else if (tree.is(Tree.Kind.EXTENDS_WILDCARD)) {
      handleBounds(Collections.singletonList(((WildcardTree) tree).bound()), tree);
    }
  }

  private void handleBounds(List<TypeTree> bounds, Tree treeToReport) {
    bounds.stream()
      .map(TypeUpperBoundNotFinalCheck::getIdentifier)
      .flatMap(Optional::stream)
      .filter(bound -> isFinal(bound) && !inOverridingMethodDeclaration(treeToReport))
      .findFirst()
      .ifPresent(identifierTree -> reportIssue(treeToReport, "Replace this type parametrization by the 'final' type `" + identifierTree.name() + "`."));
  }

  private static Optional<IdentifierTree> getIdentifier(TypeTree t) {
    if (t.is(Tree.Kind.IDENTIFIER)) {
      return Optional.of((IdentifierTree) t);
    } else if (t.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      ParameterizedTypeTree type = (ParameterizedTypeTree) t;
      return getIdentifier(type.type());
    }

    return Optional.empty();
  }

  private static boolean isFinal(IdentifierTree bound) {
    return bound.symbol().isFinal();
  }

  /**
   * Returns true if 'type' is part of the signature of an overriding method.
   */
  private static boolean inOverridingMethodDeclaration(Tree type) {
    return getMethodDeclaration(type).filter(TypeUpperBoundNotFinalCheck::isOverriding).isPresent();
  }

  /**
   * Returns the method where 'type' appears in its signature, if any.
   */
  private static Optional<MethodTree> getMethodDeclaration(Tree type) {
    Tree parent = type.parent();
    while (parent != null && !parent.is(Tree.Kind.BLOCK)) {
      if (parent.is(Tree.Kind.METHOD)) {
        return Optional.of((MethodTree) parent);
      }
      parent = parent.parent();
    }
    return Optional.empty();
  }

  private static boolean isOverriding(MethodTree method) {
    return !Boolean.FALSE.equals(method.isOverriding());
  }
}
