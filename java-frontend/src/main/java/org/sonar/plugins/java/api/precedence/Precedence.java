/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

package org.sonar.plugins.java.api.precedence;

import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

public enum Precedence {

  ATOM, POSTFIX, UNARY, MULTIPLICATIVE, ADDITIVE, SHIFT, RELATIONAL, EQUALITY, BITWISE_AND,
  BITWISE_XOR, BITWISE_OR, LOGICAL_AND, LOGICAL_OR, TERNARY, ASSIGNMENT;

  public boolean bindsStrongerThan(Precedence that) {
    return this.ordinal() < that.ordinal();
  }

  public static Precedence precedence(ExpressionTree expr) {
    return precedence(expr.kind());
  }

  public static Precedence precedence(Tree.Kind kind) {
    return switch (kind) {
      case POSTFIX_INCREMENT, POSTFIX_DECREMENT -> POSTFIX;
      case PREFIX_INCREMENT, PREFIX_DECREMENT, UNARY_PLUS, UNARY_MINUS, BITWISE_COMPLEMENT, LOGICAL_COMPLEMENT -> UNARY;
      case MULTIPLY, DIVIDE, REMAINDER -> MULTIPLICATIVE;
      case PLUS, MINUS -> ADDITIVE;
      case LEFT_SHIFT, RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> SHIFT;
      case LESS_THAN, GREATER_THAN, LESS_THAN_OR_EQUAL_TO, GREATER_THAN_OR_EQUAL_TO, INSTANCE_OF, PATTERN_INSTANCE_OF -> RELATIONAL;
      case EQUAL_TO, NOT_EQUAL_TO -> EQUALITY;
      case AND -> BITWISE_AND;
      case XOR -> BITWISE_XOR;
      case OR -> BITWISE_OR;
      case CONDITIONAL_AND -> LOGICAL_AND;
      case CONDITIONAL_OR -> LOGICAL_OR;
      case CONDITIONAL_EXPRESSION -> TERNARY;
      case ASSIGNMENT, PLUS_ASSIGNMENT, MINUS_ASSIGNMENT, MULTIPLY_ASSIGNMENT, DIVIDE_ASSIGNMENT, REMAINDER_ASSIGNMENT,
           AND_ASSIGNMENT, XOR_ASSIGNMENT, OR_ASSIGNMENT, LEFT_SHIFT_ASSIGNMENT, RIGHT_SHIFT_ASSIGNMENT,
           UNSIGNED_RIGHT_SHIFT_ASSIGNMENT -> ASSIGNMENT;
      default -> ATOM;
    };
  }

}
