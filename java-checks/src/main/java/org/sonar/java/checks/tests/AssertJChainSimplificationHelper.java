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
package org.sonar.java.checks.tests;

import java.util.function.Predicate;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

class AssertJChainSimplificationHelper {

  private AssertJChainSimplificationHelper() {
    // Hide default constructor
  }

  static class BooleanFlag {
    private boolean flag = false;

    public void setTrue() {
      flag = true;
    }

    public boolean value() {
      return flag;
    }
  }

  static class ArgumentHelper {

    private ArgumentHelper() {
      // Hide default constructor
    }

    static boolean equalsTo(ExpressionTree expression, Predicate<ExpressionTree> comparedWithPredicate) {
      return expression.is(Tree.Kind.EQUAL_TO) && leftOrRightIs((BinaryExpressionTree) expression, comparedWithPredicate);
    }

    static boolean notEqualsTo(ExpressionTree expression, Predicate<ExpressionTree> comparedWithPredicate) {
      return expression.is(Tree.Kind.NOT_EQUAL_TO) && leftOrRightIs((BinaryExpressionTree) expression, comparedWithPredicate);
    }

    static boolean leftOrRightIs(BinaryExpressionTree bet, Predicate<ExpressionTree> sidePredicate) {
      return sidePredicate.test(bet.leftOperand()) || sidePredicate.test(bet.rightOperand());
    }
  }

  static boolean hasMethodCallAsArg(ExpressionTree arg, MethodMatchers methodCallMatcher) {
    if (arg.is(Tree.Kind.METHOD_INVOCATION)) {
      return methodCallMatcher.matches((MethodInvocationTree) arg);
    }
    return false;
  }
}
