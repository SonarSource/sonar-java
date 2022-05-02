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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

@Rule(key = "S4968")
public class TypeUpperBoundNotFinalCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.TYPE_PARAMETER, Tree.Kind.EXTENDS_WILDCARD);
  }

  @Override
  public void visitNode(Tree tree) {
    List<TypeTree> bounds = getBounds(tree);
    for (TypeTree bound: bounds) {
      reportIssueIfTypeIsFinal(bound, tree);
    }
  }

  private List<TypeTree> getBounds(Tree tree) {
    if (tree.is(Tree.Kind.TYPE_PARAMETER)) {
      TypeParameterTree typeParamTree = (TypeParameterTree) tree;
      return typeParamTree.bounds();
    } else {
      List<TypeTree> bounds = new LinkedList<>();
      bounds.add(((WildcardTree)tree).bound());
      return bounds;
    }
  }

  void reportIssueIfTypeIsFinal(TypeTree type, Tree containingTree) {
    if (type.is(Tree.Kind.IDENTIFIER)) {
      if (((IdentifierTree)type).symbol().isFinal()) {
        reportIssue(containingTree, "The upper bound of a type variable should not be final.");
      }
    } else {
      reportIssueIfTypeIsFinal(((ParameterizedTypeTree) type).type(), containingTree);
    }
  }
}
