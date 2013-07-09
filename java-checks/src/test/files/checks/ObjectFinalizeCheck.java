class Foo {
  void foo() {
    Foo foo = new Foo();
    foo.finalize();       // Non-Compliant
    foo.finalize[0];      // Compliant
    foo.finalize(0);      // Non-Compliant
    foo.toString();       // Compliant
    foo.finalize;         // Compliant
    super.finalize();     // Compliant
    this.finalize();      // Compliant
    finalize();           // Non-Compliant
  }
}
