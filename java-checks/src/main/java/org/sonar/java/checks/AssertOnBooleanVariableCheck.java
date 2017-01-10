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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssertStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(key = "S3346")
public class AssertOnBooleanVariableCheck extends IssuableSubscriptionVisitor {

  private static final List<Kind> SIDE_EFFECT_KIND = ImmutableList.of(
    Kind.METHOD_INVOCATION,

    Kind.ASSIGNMENT,
    Kind.MULTIPLY_ASSIGNMENT,
    Kind.DIVIDE_ASSIGNMENT,
    Kind.REMAINDER_ASSIGNMENT,
    Kind.PLUS_ASSIGNMENT,
    Kind.MINUS_ASSIGNMENT,
    Kind.LEFT_SHIFT_ASSIGNMENT,
    Kind.RIGHT_SHIFT_ASSIGNMENT,
    Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
    Kind.AND_ASSIGNMENT,
    Kind.XOR_ASSIGNMENT,
    Kind.OR_ASSIGNMENT,

    Kind.POSTFIX_DECREMENT,
    Kind.POSTFIX_INCREMENT,
    Kind.PREFIX_DECREMENT,
    Kind.PREFIX_INCREMENT
  );

  private boolean withinAssert;
  private boolean containsSideEffect;

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.<Kind>builder()
      .add(Kind.ASSERT_STATEMENT)
      .addAll(SIDE_EFFECT_KIND)
      .build();
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.ASSERT_STATEMENT)) {
      withinAssert = true;
      containsSideEffect = false;
    } else if (withinAssert && !containsSideEffect) {
      containsSideEffect = true;
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Kind.ASSERT_STATEMENT)) {
      if (containsSideEffect) {
        reportIssue(((AssertStatementTree) tree).condition(), "Move this \"assert\" side effect to another statement.");
      }
      withinAssert = false;
    }
  }
}
