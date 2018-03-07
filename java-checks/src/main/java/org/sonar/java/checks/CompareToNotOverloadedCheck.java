/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.resolve.ClassJavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S4351")
public class CompareToNotOverloadedCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    ClassJavaType clt;
    String name = "Object";
    if (hasSemantic() && isCompareToMethod(methodTree) && Boolean.FALSE.equals(methodTree.isOverriding())) {
      clt = (ClassJavaType) methodTree.symbol().owner().type();
      if (isComparableImplemented(clt)) {
        ClassJavaType comparableType = clt.superTypes().stream().filter(supertype -> supertype.is("java.lang.Comparable")).findFirst().get();
        if (comparableType.isParameterized()) {
          ParametrizedTypeJavaType ptjt = (ParametrizedTypeJavaType) comparableType;
          name = ptjt.substitution(ptjt.typeParameters().get(0)).symbol().name();
        }
        reportIssue((Tree) methodTree.parameters(), "Refactor this method so that its argument is of type '" + name + "'.");
      }
    }
  }

  private static boolean isCompareToMethod(MethodTree tree) {
    return "compareTo".equals(tree.simpleName().name()) && tree.parameters().size() == 1;
  }

  private static boolean isComparableImplemented(ClassJavaType classJavaType) {
    return classJavaType.isSubtypeOf("java.lang.Comparable");
  }

}
