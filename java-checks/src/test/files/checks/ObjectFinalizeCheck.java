class Foo {
  void foo() {
    Foo foo = new Foo();
    foo.finalize();       // Non-Compliant
    foo.finalize[0];      // Compliant
    foo.finalize(0);      // Compliant
    foo.toString();       // Compliant
    foo.finalize;         // Compliant
    super.finalize();     // Non-Compliant
    this.finalize();      // Non-Compliant
    finalize();           // Non-Compliant
    finalize() + 0;       // Compliant
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();     // Compliant
  }

  public int foo() {
    return 0;
  }
}
