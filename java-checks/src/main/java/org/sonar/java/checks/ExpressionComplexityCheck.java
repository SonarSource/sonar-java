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
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

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
  public void scanFile(JavaFileScannerContext context) {
    count.clear();
    level.clear();
    level.push(0);
    count.push(0);
    super.scanFile(context);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.<Tree.Kind>builder()
        .add(Tree.Kind.CLASS)
        .add(Tree.Kind.POSTFIX_INCREMENT)
        .add(Tree.Kind.POSTFIX_DECREMENT)
        .add(Tree.Kind.PREFIX_INCREMENT)
        .add(Tree.Kind.PREFIX_DECREMENT)
        .add(Tree.Kind.UNARY_PLUS)
        .add(Tree.Kind.UNARY_MINUS)
        .add(Tree.Kind.BITWISE_COMPLEMENT)
        .add(Tree.Kind.LOGICAL_COMPLEMENT)
        .add(Tree.Kind.MULTIPLY)
        .add(Tree.Kind.DIVIDE)
        .add(Tree.Kind.REMAINDER)
        .add(Tree.Kind.PLUS)
        .add(Tree.Kind.MINUS)
        .add(Tree.Kind.LEFT_SHIFT)
        .add(Tree.Kind.RIGHT_SHIFT)
        .add(Tree.Kind.UNSIGNED_RIGHT_SHIFT)
        .add(Tree.Kind.LESS_THAN)
        .add(Tree.Kind.GREATER_THAN)
        .add(Tree.Kind.LESS_THAN_OR_EQUAL_TO)
        .add(Tree.Kind.GREATER_THAN_OR_EQUAL_TO)
        .add(Tree.Kind.EQUAL_TO)
        .add(Tree.Kind.NOT_EQUAL_TO)
        .add(Tree.Kind.AND)
        .add(Tree.Kind.XOR)
        .add(Tree.Kind.OR)
        .add(Tree.Kind.CONDITIONAL_AND)
        .add(Tree.Kind.CONDITIONAL_OR)
        .add(Tree.Kind.CONDITIONAL_EXPRESSION)
        .add(Tree.Kind.ARRAY_ACCESS_EXPRESSION)
        .add(Tree.Kind.MEMBER_SELECT)
        .add(Tree.Kind.NEW_CLASS)
        .add(Tree.Kind.NEW_ARRAY)
        .add(Tree.Kind.METHOD_INVOCATION)
        .add(Tree.Kind.TYPE_CAST)
        .add(Tree.Kind.INSTANCE_OF)
        .add(Tree.Kind.PARENTHESIZED_EXPRESSION)
        .add(Tree.Kind.ASSIGNMENT)
        .add(Tree.Kind.MULTIPLY_ASSIGNMENT)
        .add(Tree.Kind.DIVIDE_ASSIGNMENT)
        .add(Tree.Kind.REMAINDER_ASSIGNMENT)
        .add(Tree.Kind.PLUS_ASSIGNMENT)
        .add(Tree.Kind.MINUS_ASSIGNMENT)
        .add(Tree.Kind.LEFT_SHIFT_ASSIGNMENT)
        .add(Tree.Kind.RIGHT_SHIFT_ASSIGNMENT)
        .add(Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT)
        .add(Tree.Kind.AND_ASSIGNMENT)
        .add(Tree.Kind.XOR_ASSIGNMENT)
        .add(Tree.Kind.OR_ASSIGNMENT)
        .add(Tree.Kind.INT_LITERAL)
        .add(Tree.Kind.LONG_LITERAL)
        .add(Tree.Kind.FLOAT_LITERAL)
        .add(Tree.Kind.DOUBLE_LITERAL)
        .add(Tree.Kind.BOOLEAN_LITERAL)
        .add(Tree.Kind.CHAR_LITERAL)
        .add(Tree.Kind.STRING_LITERAL)
        .add(Tree.Kind.NULL_LITERAL)
        .add(Tree.Kind.IDENTIFIER)
        .add(Tree.Kind.ARRAY_TYPE)
        .add(Tree.Kind.LAMBDA_EXPRESSION)
        .add(Tree.Kind.PRIMITIVE_TYPE)
        .build();
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.NEW_ARRAY)) {
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
    if (tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.NEW_ARRAY)) {
      count.pop();
      level.pop();
    } else {
      int currentLevel = level.peek();
      if (currentLevel == 1) {
        int opCount = count.pop();
        if (opCount > max) {
          reportIssue(tree, "Reduce the number of conditional operators (" + opCount + ") used in the expression (maximum allowed " + max + ").",
            Collections.<JavaFileScannerContext.Location>emptyList(), opCount - max);
        }
        count.push(0);
      }
      level.push(level.pop() - 1);
    }
  }

}
