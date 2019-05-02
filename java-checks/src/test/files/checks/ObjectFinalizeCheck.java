class Foo {

  int[] finalize;
  Object x;

  void foo() {
    Foo foo = new Foo();
    foo.finalize();       // Noncompliant [[sc=9;ec=17]] {{Remove this call to finalize().}}
    x = foo.finalize[0];  // Compliant
    foo.finalize(0);      // Compliant
    foo.toString();       // Compliant
    x = foo.finalize;     // Compliant
    super.finalize();     // Noncompliant {{Remove this call to finalize().}}
    this.finalize();      // Noncompliant {{Remove this call to finalize().}}
    finalize();           // Noncompliant {{Remove this call to finalize().}}
    x = finalize() + 0;   // Noncompliant {{Remove this call to finalize().}}
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();     // Compliant
  }

  public int foo() {
    return 0;
  }

  void finalize(int i) { }

}
