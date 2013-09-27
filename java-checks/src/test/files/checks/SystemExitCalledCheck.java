class A {
  void f() {
    System.exit(0);          // Non-Compliant
    int a = System.exit(0);  // Non-Compliant
    System.gc();             // Compliant
    System.exit[0];          // Compliant
    exit();                  // Compliant
    Runtime.getRuntime().exit(); // Non-Compliant
    Runtime.getRuntime().foo; // Compliant
    Runtime.getRuntime().foo(); // Compliant
    Runtime.getRuntime()++; // Compliant
  }
}
