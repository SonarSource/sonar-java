class Foo {

  @Override
  protected void finalize() throws Throwable {  // Noncompliant [[sc=18;ec=26]] {{Do not override the Object.finalize() method.}}
  }

  public void foo() {                           // Compliant
  }

  @Override
  protected boolean finalize() {                // Compliant
  }

}
