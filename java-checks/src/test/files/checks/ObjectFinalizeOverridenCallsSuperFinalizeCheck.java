class Foo {
  @Override
  protected void finalize() throws Throwable {  // Compliant
    System.out.println("foo");
    super.finalize();
  }

  @Override
  protected void finalize() throws Throwable {  // Non-Compliant
    super.finalize();
    System.out.println("foo");
  }

  @Override
  protected void finalize() throws Throwable {  // Non-Compliant
  }

  @Override
  protected void finalize() throws Throwable {  // Non-Compliant
    System.out.println("foo");
    super.foo();
  }

  @Override
  protected void foo() throws Throwable {       // Compliant
  }

  boolean finalize() {                          // Compliant
  }
}
