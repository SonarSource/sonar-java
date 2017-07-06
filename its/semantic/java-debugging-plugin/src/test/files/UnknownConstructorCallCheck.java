class A {
  void foo() {
    new A(); // Compliant
    new B(); // Noncompliant
  }
}
