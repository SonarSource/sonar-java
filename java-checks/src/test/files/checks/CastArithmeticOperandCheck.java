class A {
  void foo() {
    long l1 = 1000 * 3600 * 24 * 365; // Noncompliant {{Cast one of the operands of this multiplication operation to a "long".}}
    long l2 = 1000L * 3600 * 24 * 365;
    float f1 = 2 / 3; // Noncompliant {{Cast one of the operands of this division operation to a "float".}}
    float f2 = 2f / 3;
    l2 = 1000 * 3600 * 24 * 365; // Noncompliant {{Cast one of the operands of this multiplication operation to a "long".}}
    l2 = 1000L * 3600 * 24 * 365; // compliant
    double d = 2 / 3; // Noncompliant {{Cast one of the operands of this division operation to a "double".}}
    long l3 = 2 + Integer.MAX_VALUE; // Noncompliant {{Cast one of the operands of this addition operation to a "long".}}
    l3 = 2 - Integer.MIN_VALUE; // Noncompliant {{Cast one of the operands of this substraction operation to a "long".}}
    longMethod(1 + 2, 1 + 2);   // Noncompliant {{Cast one of the operands of this addition operation to a "long".}}
    longMethod(1 + 2, 1 + 2l);  // Compliant
    doubleMethod(1 + 2, 1 + 2); // Noncompliant {{Cast one of the operands of this addition operation to a "double".}}
    doubleMethod(1 + 2, 1 + 2d);// Compliant
    floatMethod(1 + 2, 1 + 2);  // Noncompliant {{Cast one of the operands of this addition operation to a "float".}}
    floatMethod(1 + 2, 1 + 2f); // Compliant
    foo(); //Compliant
  }


  void longMethod(int a, long l) {}
  void doubleMethod(int a, double d) {}
  void floatMethod(int a, float f) {}

  long l() {
    if (true) {
      return 1 + 2l; // compliant
    } else {
      return 1 + 2; // Noncompliant {{Cast one of the operands of this addition operation to a "long".}}
    }
  }

  double d() {
    if (true) {
      return 1 + 2d; // compliant
    } else {
      return 1 + 2; // Noncompliant {{Cast one of the operands of this addition operation to a "double".}}
    }
  }

  float f() {
    if (true) {
      return 1 + 2f; // compliant
    } else {
      return 1 + 2; // Noncompliant {{Cast one of the operands of this addition operation to a "float".}}
    }
  }
}