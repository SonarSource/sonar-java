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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S1160")
public class ThrowsSeveralCheckedExceptionCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (hasSemantic() && isPublic(methodTree) && !((MethodTreeImpl) methodTree).isMainMethod()) {
      List<String> thrownCheckedExceptions = getThrownCheckedExceptions(methodTree);
      if (thrownCheckedExceptions.size() > 1 && isNotOverriden(methodTree)) {
        reportIssue(methodTree.simpleName(), "Refactor this method to throw at most one checked exception instead of: " + Joiner.on(", ").join(thrownCheckedExceptions));
      }
    }
  }

  private static boolean isNotOverriden(MethodTree methodTree) {
    return BooleanUtils.isFalse(((MethodTreeImpl) methodTree).isOverriding());
  }

  private static boolean isPublic(MethodTree methodTree) {
    return methodTree.symbol().isPublic();
  }

  private static List<String> getThrownCheckedExceptions(MethodTree methodTree) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (Type thrownClass : methodTree.symbol().thrownTypes()) {
      if (!thrownClass.isUnknown() && !isSubClassOfRuntimeException(thrownClass)) {
        builder.add(thrownClass.fullyQualifiedName());
      }
    }
    return builder.build();
  }

  private static boolean isSubClassOfRuntimeException(Type thrownClass) {
    Symbol.TypeSymbol typeSymbol = thrownClass.symbol();
    while (typeSymbol != null) {
      if (isRuntimeException(typeSymbol.type())) {
        return true;
      }
      Type superType = typeSymbol.superClass();
      if (superType == null) {
        typeSymbol = null;
      } else {
        typeSymbol = superType.symbol();
      }
    }
    return false;
  }

  private static boolean isRuntimeException(Type thrownClass) {
    return thrownClass.is("java.lang.RuntimeException");
  }

}
