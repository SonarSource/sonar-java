class Foo {

  @Override
  protected void finalize() throws Throwable {  // Non-Compliant
  }

  public void foo() {                           // Compliant
  }

  @Override
  protected boolean finalize() {                // Compliant
  }

}
