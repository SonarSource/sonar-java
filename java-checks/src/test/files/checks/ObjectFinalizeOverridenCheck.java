class Foo {

  @Override
  protected void finalize() throws Throwable {  // Noncompliant {{Do not override the Object.finalize() method.}}
  }

  public void foo() {                           // Compliant
  }

  @Override
  protected boolean finalize() {                // Compliant
  }

}
