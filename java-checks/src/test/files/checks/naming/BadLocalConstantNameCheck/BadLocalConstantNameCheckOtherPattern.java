class A {

  void foo() {
    final int MY_LOCAL_CONST = 42; // Noncompliant
    final int MYLOCALCONST = 42; // Compliant
  }
}
