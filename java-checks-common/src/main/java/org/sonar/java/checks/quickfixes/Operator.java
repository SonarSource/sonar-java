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

package org.sonar.java.checks.quickfixes;

public sealed interface Operator {
  // Reference: https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html

  String code();
  Precedence precedence();
  boolean isAssociative();

  enum PostfixOperator implements Operator {
    POST_INC("++"), POST_DEC("--");

    private final String code;

    PostfixOperator(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    @Override
    public Precedence precedence() {
      return Precedence.POSTFIX;
    }

    @Override
    public boolean isAssociative() {
      return false;
    }

  }

  enum UnaryOperator implements Operator {
    PRE_INC("++"), PRE_DEC("--"), UNARY_PLUS("+"),
    UNARY_MINUS("-"), BITWISE_NOT("~"), NOT("!");

    private final String code;

    UnaryOperator(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    @Override
    public Precedence precedence() {
      return Precedence.UNARY;
    }

    @Override
    public boolean isAssociative() {
      return false;
    }

  }

  enum BinaryOperator implements Operator {
    MUL("*", Precedence.MULTIPLICATIVE, true), DIV("/", Precedence.MULTIPLICATIVE, false),
    MOD("%", Precedence.MULTIPLICATIVE, false),

    ADD("+", Precedence.ADDITIVE, true), SUB("-", Precedence.ADDITIVE, false),

    SHIFTL("<<", Precedence.SHIFT, false), SHIFTR_ARITH(">>", Precedence.SHIFT, false),
    SHIFTR_LOGICAL(">>>", Precedence.SHIFT, false),

    LT("<", Precedence.RELATIONAL, false), GT(">", Precedence.RELATIONAL, false),
    LEQ("<=", Precedence.RELATIONAL, false), GEQ(">=", Precedence.RELATIONAL, false),

    EQUALITY("==", Precedence.EQUALITY, false), INEQUALITY("!=", Precedence.EQUALITY, false),

    BITW_AND("&", Precedence.BITWISE_AND, true),

    BITW_XOR("^", Precedence.BITWISE_XOR, true),

    BITW_OR("|", Precedence.BITWISE_OR, true),

    AND("&&", Precedence.AND, true),

    OR("||", Precedence.OR, true);

    private final String code;
    private final Precedence precedence;
    private final boolean isAssociative;

    BinaryOperator(String code, Precedence precedence, boolean isAssociative) {
      this.code = code;
      this.precedence = precedence;
      this.isAssociative = isAssociative;
    }

    @Override
    public String code() {
      return code;
    }

    @Override
    public Precedence precedence() {
      return precedence;
    }

    @Override
    public boolean isAssociative() {
      return isAssociative;
    }

  }

  // ternary has its own implementation

  enum AssignmentOperator implements Operator {
    ASSIG("="), ADD_ASSIG("+="), SUB_ASSIG("-="),
    MUL_ASSIG("%="), DIV_ASSIG("/="), MOD_ASSIG("%="),
    AND_ASSIG("&="), XOR_ASSIG("^="), OR_ASSIG("|="),
    SHIFTL_ASSIG("<<="), SHIFTR_ARITH_ASSIG(">>"), SHR_LOGICAL_ASSIG(">>>");

    private final String code;

    AssignmentOperator(String code) {
      this.code = code;
    }

    @Override
    public String code() {
      return code;
    }

    @Override
    public Precedence precedence() {
      return Precedence.ASSIGNMENT;
    }

    @Override
    public boolean isAssociative() {
      return false;
    }

  }

}
