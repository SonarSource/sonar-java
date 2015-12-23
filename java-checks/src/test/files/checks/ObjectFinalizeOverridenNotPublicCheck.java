class Foo {

  @Override
  protected void finalize() throws Throwable {  // Compliant
  }

  public void foo() {                           // Compliant
  }

  @Override
  protected boolean finalize() {                // Compliant
  }

  @Override
  public void finalize() throws Throwable {    // Noncompliant [[sc=15;ec=23]] {{Make this finalize() method protected.}}
  }

  Object finalize() {}
}
