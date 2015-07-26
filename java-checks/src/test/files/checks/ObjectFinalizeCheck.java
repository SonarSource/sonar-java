class Foo {
  void foo() {
    Foo foo = new Foo();
    foo.finalize();       // Noncompliant {{Remove this call to finalize().}}
    foo.finalize[0];      // Compliant
    foo.finalize(0);      // Compliant
    foo.toString();       // Compliant
    foo.finalize;         // Compliant
    super.finalize();     // Noncompliant {{Remove this call to finalize().}}
    this.finalize();      // Noncompliant {{Remove this call to finalize().}}
    finalize();           // Noncompliant {{Remove this call to finalize().}}
    finalize() + 0;       // Noncompliant {{Remove this call to finalize().}}
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();     // Compliant
  }

  public int foo() {
    return 0;
  }
}
