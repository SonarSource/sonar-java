package org.sonar.java.checks.quickfixes;

public enum Precedence {

  // ordered from weakest to strongest precedence
  ATOM, POSTFIX, UNARY, MULTIPLICATIVE, ADDITIVE, SHIFT, RELATIONAL, EQUALITY, BITWISE_AND,
  BITWISE_XOR, BITWISE_OR, AND, OR, TERNARY, ASSIGNMENT;

  boolean isStrongerThan(Precedence that){
    return this.ordinal() < that.ordinal();
  }

}
