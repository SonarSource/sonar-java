/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.design;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1200")
public class ClassCouplingCheck extends AbstractCouplingChecker {

  private static final int DEFAULT_MAX = 20;

  @RuleProperty(
    key = "max",
    description = "Maximum number of classes a single class is allowed to depend upon",
    defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  @Override
  public void visitClass(ClassTree tree) {
    if (tree.is(Tree.Kind.CLASS) && tree.simpleName() != null) {
      nesting.push(types);
      types = new HashSet<>();
    }
    checkTypes(tree.superClass());
    checkTypes(tree.superInterfaces());
    super.visitClass(tree);
    if (tree.is(Tree.Kind.CLASS) && tree.simpleName() != null) {
      if (types.size() > max) {
        context.reportIssue(
          this,
          tree.simpleName(),
          "Split this class into smaller and more specialized ones to reduce its dependencies on other classes from " +
            types.size() + " to the maximum authorized " + max + " or less.");
      }
      types = nesting.pop();
    }
  }

  @Override
  public void checkTypes(@Nullable Tree type) {
    if (type == null || types == null) {
      return;
    }
    if (type.is(Tree.Kind.IDENTIFIER)) {
      types.add(((IdentifierTree) type).name());
    } else if (type.is(Tree.Kind.MEMBER_SELECT)) {
      Deque<String> fullyQualifiedNameComponents = new ArrayDeque<>();
      ExpressionTree expr = (ExpressionTree) type;
      while (expr.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
        fullyQualifiedNameComponents.push(mse.identifier().name());
        expr = mse.expression();
      }
      if (expr.is(Tree.Kind.IDENTIFIER)) {
        fullyQualifiedNameComponents.push(((IdentifierTree) expr).name());
      }
      types.add(String.join(".", fullyQualifiedNameComponents));
    }
  }
}
