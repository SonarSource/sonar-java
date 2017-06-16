class A {
  void foo() {
    foo(); // Compliant
    bar(); // Noncompliant

    this.foo(); // Compliant
    this.bar(); // Noncompliant
  }
}
