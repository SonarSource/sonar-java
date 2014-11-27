class A {
  void foo() {
    long l1 = 1000 * 3600 * 24 * 365; // Noncompliant
    long l2 = 1000L * 3600 * 24 * 365;
    float f1 = 2 / 3; // Noncompliant
    float f2 = 2f / 3;
    l2 = 1000 * 3600 * 24 * 365; // Noncompliant
    l2 = 1000L * 3600 * 24 * 365; // compliant
    double d = 2 / 3; // Noncompliant
    long l3 = 2 + Integer.MAX_VALUE; // Noncompliant
    l3 = 2 - Integer.MIN_VALUE; // Noncompliant
    longMethod(1 + 2, 1 + 2);   // Noncompliant
    longMethod(1 + 2, 1 + 2l);  // Compliant
    doubleMethod(1 + 2, 1 + 2); // Noncompliant
    doubleMethod(1 + 2, 1 + 2d);// Compliant
    floatMethod(1 + 2, 1 + 2);  // Noncompliant
    floatMethod(1 + 2, 1 + 2f); // Compliant
    foo(); //Compliant
  }


  void longMethod(int a, long l) { }
  void doubleMethod(int a, double d) { }
  void floatMethod(int a, float f) { }
}