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

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(key = "S1067")
public class ExpressionComplexityCheck extends IssuableSubscriptionVisitor {


  private static final int DEFAULT_MAX = 3;

  @RuleProperty(
    description = "Maximum number of allowed conditional operators in an expression",
    defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private final Deque<Integer> count = new LinkedList<>();
  private final Deque<Integer> level = new LinkedList<>();

  @Override
  public void setContext(JavaFileScannerContext context) {
    count.clear();
    level.clear();
    level.push(0);
    count.push(0);
    super.setContext(context);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.CLASS,
      Tree.Kind.POSTFIX_INCREMENT,
      Tree.Kind.POSTFIX_DECREMENT,
      Tree.Kind.PREFIX_INCREMENT,
      Tree.Kind.PREFIX_DECREMENT,
      Tree.Kind.UNARY_PLUS,
      Tree.Kind.UNARY_MINUS,
      Tree.Kind.BITWISE_COMPLEMENT,
      Tree.Kind.LOGICAL_COMPLEMENT,
      Tree.Kind.MULTIPLY,
      Tree.Kind.DIVIDE,
      Tree.Kind.REMAINDER,
      Tree.Kind.PLUS,
      Tree.Kind.MINUS,
      Tree.Kind.LEFT_SHIFT,
      Tree.Kind.RIGHT_SHIFT,
      Tree.Kind.UNSIGNED_RIGHT_SHIFT,
      Tree.Kind.LESS_THAN,
      Tree.Kind.GREATER_THAN,
      Tree.Kind.LESS_THAN_OR_EQUAL_TO,
      Tree.Kind.GREATER_THAN_OR_EQUAL_TO,
      Tree.Kind.EQUAL_TO,
      Tree.Kind.NOT_EQUAL_TO,
      Tree.Kind.AND,
      Tree.Kind.XOR,
      Tree.Kind.OR,
      Tree.Kind.CONDITIONAL_AND,
      Tree.Kind.CONDITIONAL_OR,
      Tree.Kind.CONDITIONAL_EXPRESSION,
      Tree.Kind.ARRAY_ACCESS_EXPRESSION,
      Tree.Kind.MEMBER_SELECT,
      Tree.Kind.NEW_CLASS,
      Tree.Kind.NEW_ARRAY,
      Tree.Kind.METHOD_INVOCATION,
      Tree.Kind.TYPE_CAST,
      Tree.Kind.INSTANCE_OF,
      Tree.Kind.PARENTHESIZED_EXPRESSION,
      Tree.Kind.ASSIGNMENT,
      Tree.Kind.MULTIPLY_ASSIGNMENT,
      Tree.Kind.DIVIDE_ASSIGNMENT,
      Tree.Kind.REMAINDER_ASSIGNMENT,
      Tree.Kind.PLUS_ASSIGNMENT,
      Tree.Kind.MINUS_ASSIGNMENT,
      Tree.Kind.LEFT_SHIFT_ASSIGNMENT,
      Tree.Kind.RIGHT_SHIFT_ASSIGNMENT,
      Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
      Tree.Kind.AND_ASSIGNMENT,
      Tree.Kind.XOR_ASSIGNMENT,
      Tree.Kind.OR_ASSIGNMENT,
      Tree.Kind.INT_LITERAL,
      Tree.Kind.LONG_LITERAL,
      Tree.Kind.FLOAT_LITERAL,
      Tree.Kind.DOUBLE_LITERAL,
      Tree.Kind.BOOLEAN_LITERAL,
      Tree.Kind.CHAR_LITERAL,
      Tree.Kind.STRING_LITERAL,
      Tree.Kind.NULL_LITERAL,
      Tree.Kind.IDENTIFIER,
      Tree.Kind.ARRAY_TYPE,
      Tree.Kind.LAMBDA_EXPRESSION,
      Tree.Kind.PRIMITIVE_TYPE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS, Tree.Kind.NEW_ARRAY) || isLambdaWithBlock(tree)) {
      count.push(0);
      level.push(0);
    } else {
      if (tree.is(Tree.Kind.CONDITIONAL_OR) || tree.is(Tree.Kind.CONDITIONAL_AND) || tree.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
        count.push(count.pop() + 1);
      }
      level.push(level.pop() + 1);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS, Tree.Kind.NEW_ARRAY) || isLambdaWithBlock(tree)) {
      count.pop();
      level.pop();
    } else {
      int currentLevel = level.peek();
      if (currentLevel == 1) {
        int opCount = count.pop();
        if (opCount > max && !isInsideEquals(tree)) {
          reportIssue(tree, "Reduce the number of conditional operators (" + opCount + ") used in the expression (maximum allowed " + max + ").",
            Collections.<JavaFileScannerContext.Location>emptyList(), opCount - max);
        }
        count.push(0);
      }
      level.push(level.pop() - 1);
    }
  }

  private static boolean isInsideEquals(Tree tree) {
    Tree parent = tree.parent();
    while (parent != null && !parent.is(Tree.Kind.CLASS)) {
      if (parent.is(Tree.Kind.METHOD) && MethodTreeUtils.isEqualsMethod((MethodTree) parent)) {
        return true;
      }
      parent = parent.parent();
    }
    return false;
  }

  private static boolean isLambdaWithBlock(Tree tree) {
    return tree.is(Tree.Kind.LAMBDA_EXPRESSION) && ((LambdaExpressionTree) tree).body().is(Tree.Kind.BLOCK);
  }
}
