/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = "RedundantThrowsDeclarationCheck")
@RspecKey("S1130")
public class RedundantThrowsDeclarationCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    ListTree<TypeTree> thrownList = ((MethodTree) tree).throwsClauses();
    Set<String> reported = new HashSet<>();
    for (TypeTree typeTree : thrownList) {
      Type symbolType = typeTree.symbolType();
      String fullyQualifiedName = symbolType.fullyQualifiedName();
      if (!reported.contains(fullyQualifiedName)) {
        String superTypeName = isSubclassOfAny(symbolType, thrownList);
        if (superTypeName != null) {
          reportIssue(typeTree, "Remove the declaration of thrown exception '" + fullyQualifiedName + "' which is a subclass of '" + superTypeName + "'.");
        } else if (symbolType.isSubtypeOf("java.lang.RuntimeException")) {
          reportIssue(typeTree, "Remove the declaration of thrown exception '" + fullyQualifiedName + "' which is a runtime exception.");
        } else if (declaredMoreThanOnce(fullyQualifiedName, thrownList)) {
          reportIssue(typeTree, "Remove the redundant '" + fullyQualifiedName + "' thrown exception declaration(s).");
        }
        reported.add(fullyQualifiedName);
      }
    }
  }

  private static boolean declaredMoreThanOnce(String fullyQualifiedName, ListTree<TypeTree> thrown) {
    boolean firstOccurrenceFound = false;
    for (TypeTree typeTree : thrown) {
      if (typeTree.symbolType().is(fullyQualifiedName)) {
        if (firstOccurrenceFound) {
          return true;
        } else {
          firstOccurrenceFound = true;
        }
      }
    }
    return false;
  }

  private static String isSubclassOfAny(Type type, ListTree<TypeTree> thrownList) {
    for (TypeTree thrown : thrownList) {
      String name = thrown.symbolType().fullyQualifiedName();
      if (!type.is(name) && type.isSubtypeOf(name)) {
        return name;
      }
    }
    return null;
  }
}
