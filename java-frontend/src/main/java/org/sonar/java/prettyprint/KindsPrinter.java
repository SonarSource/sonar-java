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

package org.sonar.java.prettyprint;

import org.sonar.plugins.java.api.tree.Tree;

public final class KindsPrinter {

  private KindsPrinter() {
  }

  public static String printExprKind(Tree.Kind expressionKind){
    return switch (expressionKind){
      case POSTFIX_INCREMENT, PREFIX_INCREMENT -> "++";
      case POSTFIX_DECREMENT, PREFIX_DECREMENT -> "--";
      case UNARY_PLUS, PLUS -> "+";
      case UNARY_MINUS, MINUS -> "-";
      case BITWISE_COMPLEMENT -> "~";
      case LOGICAL_COMPLEMENT -> "!";
      case MULTIPLY -> "*";
      case DIVIDE -> "/";
      case REMAINDER -> "%";
      case LEFT_SHIFT -> "<<";
      case RIGHT_SHIFT -> ">>";
      case UNSIGNED_RIGHT_SHIFT -> ">>>";
      case LESS_THAN -> "<";
      case GREATER_THAN -> ">";
      case LESS_THAN_OR_EQUAL_TO -> "<=";
      case GREATER_THAN_OR_EQUAL_TO -> ">=";
      case EQUAL_TO -> "==";
      case NOT_EQUAL_TO -> "!=";
      case AND -> "&";
      case XOR -> "^";
      case OR -> "|";
      case CONDITIONAL_AND -> "&&";
      case CONDITIONAL_OR -> "||";
      case ASSIGNMENT -> "=";
      case MULTIPLY_ASSIGNMENT -> "*=";
      case DIVIDE_ASSIGNMENT -> "/=";
      case REMAINDER_ASSIGNMENT -> "%=";
      case PLUS_ASSIGNMENT -> "+=";
      case MINUS_ASSIGNMENT -> "-=";
      case LEFT_SHIFT_ASSIGNMENT -> "<<=";
      case RIGHT_SHIFT_ASSIGNMENT -> ">>=";
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT -> ">>>=";
      case AND_ASSIGNMENT -> "&=";
      case XOR_ASSIGNMENT -> "^=";
      case OR_ASSIGNMENT -> "|=";
      default -> throw new IllegalArgumentException("unexpected: " + expressionKind);
    };
  }

}
