package org.sonar.java.checks.prettyprint;

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
