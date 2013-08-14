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
  public void finalize() throws Throwable {    // Non-Compliant
  }

}
