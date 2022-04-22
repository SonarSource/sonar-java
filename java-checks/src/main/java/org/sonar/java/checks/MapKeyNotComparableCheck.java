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
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

@Rule(key = "S6411")
public class MapKeyNotComparableCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.PARAMETERIZED_TYPE);
  }

  @Override
  public void visitNode(Tree tree) {
    ParameterizedTypeTree typeTree = (ParameterizedTypeTree) tree;
    Type type = typeTree.symbolType();
    if (type.isSubtypeOf("java.util.Map")) {
      getMapKeyTree(typeTree).ifPresent(mapKeyTree -> {
        if (!isGenericOrWildCard(mapKeyTree) && !implementsComparable(mapKeyTree.symbolType())) {
          reportIssue(mapKeyTree, "The key type should implement Comparable.");
        }
      });
    }
  }

  private static boolean isGenericOrWildCard(TypeTree tree) {
    return JUtils.isTypeVar(tree.symbolType()) || tree instanceof WildcardTree;
  }

  private static Optional<TypeTree> getMapKeyTree(ParameterizedTypeTree typeTree) {
    TypeArguments typeArgs = typeTree.typeArguments();
    if (typeArgs.size() == 2) {
      TypeTree mapKeyTree = typeArgs.get(0);
      return Optional.of(mapKeyTree);
    }
    return Optional.empty();
  }

  private static boolean implementsComparable(Type mapKeyType) {
    return mapKeyType.isSubtypeOf("java.lang.Comparable");
  }
}
