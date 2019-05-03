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

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S1162")
public class ThrowCheckedExceptionCheck extends IssuableSubscriptionVisitor {

  private Deque<MethodTree> methods = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.THROW_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      methods.push((MethodTree) tree);
    } else {
      ThrowStatementTree throwStatementTree = (ThrowStatementTree) tree;
      Type symbolType = throwStatementTree.expression().symbolType();
      if (symbolType.isSubtypeOf("java.lang.Exception") && !symbolType.isSubtypeOf("java.lang.RuntimeException") && !isFromMethodOverride(symbolType)) {
        reportIssue(throwStatementTree.expression(), "Remove the usage of the checked exception '" + symbolType.name() + "'.");
      }
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      methods.pop();
    }
  }

  private boolean isFromMethodOverride(Type exceptionType) {
    if (!methods.isEmpty()) {
      MethodTree method = methods.peek();
      if (isOverriding(method) && isCompatibleWithThrows(exceptionType, method.throwsClauses())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isCompatibleWithThrows(Type exceptionType, List<TypeTree> throwsClauses) {
    for (TypeTree typeTree : throwsClauses) {
      if (exceptionType.isSubtypeOf(typeTree.symbolType())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isOverriding(MethodTree methodTree) {
    return Boolean.TRUE.equals(methodTree.isOverriding());
  }
}
