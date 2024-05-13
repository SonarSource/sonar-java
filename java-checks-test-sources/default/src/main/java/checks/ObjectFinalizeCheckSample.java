package checks;

class ObjectFinalizeCheckSample {

  public static class Foo {
    int[] finalize;

    @Override
    protected void finalize() throws Throwable {
      super.finalize();     // Compliant
    }

    void finalize(int i) {}
  }

  Object x;

  void foo() throws Throwable {
    Foo foo = new Foo();
    foo.finalize(); // Noncompliant {{Remove this call to finalize().}}
//      ^^^^^^^^
    x = foo.finalize[0];  // Compliant
    foo.finalize(0);      // Compliant
    foo.toString();       // Compliant
    x = foo.finalize;     // Compliant
    super.finalize(); // Noncompliant {{Remove this call to finalize().}}
    this.finalize(); // Noncompliant {{Remove this call to finalize().}}
    finalize(); // Noncompliant {{Remove this call to finalize().}}
  }

  public int bar() {
    return 0;
  }
}
