package org.sonar.java.checks.quickfixes;

public enum Precedence {

  ATOM, POSTFIX, UNARY, MULTIPLICATIVE, ADDITIVE, SHIFT, RELATIONAL, EQUALITY, BITWISE_AND,
  BITWISE_XOR, BITWISE_OR, AND, OR, TERNARY, ASSIGNMENT;

  boolean hasPrecedenceOver(Precedence that){
    return this.ordinal() < that.ordinal();
  }

}
