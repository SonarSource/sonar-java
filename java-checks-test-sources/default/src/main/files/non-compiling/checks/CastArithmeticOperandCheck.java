package checks;

class CastArithmeticOperandCheck {

  CastArithmeticOperandCheck(int a, long l) {}

  void foo() {
    longMethod(1 + 2, 1 + 2); // Noncompliant
    unknownMethod(1 + 2); // Compliant
  }
  void longMethod(int a, long l) {}
}

