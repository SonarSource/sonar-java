package org.sonar.java.checks.quickfixes;

public sealed interface Operator {
  // Reference: https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html

  String code();
  Precedence precedence();

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
  }

  enum BinaryOperator implements Operator {
    MUL("*", Precedence.MULTIPLICATIVE), DIV("/", Precedence.MULTIPLICATIVE), MOD("%", Precedence.MULTIPLICATIVE),

    ADD("+", Precedence.ADDITIVE), SUB("-", Precedence.ADDITIVE),

    SHIFTL("<<", Precedence.SHIFT), SHIFTR_ARITH(">>", Precedence.SHIFT), SHIFTR_LOGICAL(">>>", Precedence.SHIFT),

    LT("<", Precedence.RELATIONAL), GT(">", Precedence.RELATIONAL), LEQ("<=", Precedence.RELATIONAL),
    GEQ(">=", Precedence.RELATIONAL),

    EQUALITY("==", Precedence.EQUALITY), INEQUALITY("!=", Precedence.EQUALITY),

    BITW_AND("&", Precedence.BITWISE_AND),

    BITW_XOR("^", Precedence.BITWISE_XOR),

    BITW_OR("|", Precedence.BITWISE_OR),

    AND("&&", Precedence.AND),

    OR("||", Precedence.OR);

    private final String code;
    private final Precedence precedence;

    BinaryOperator(String code, Precedence precedence) {
      this.code = code;
      this.precedence = precedence;
    }

    @Override
    public String code() {
      return code;
    }

    @Override
    public Precedence precedence() {
      return precedence;
    }
  }

  // ternary has its own implementation (precedence: 101)

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
  }

}
