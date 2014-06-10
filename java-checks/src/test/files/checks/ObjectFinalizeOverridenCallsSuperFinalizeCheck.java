class Foo {
  @Override
  protected void finalize() throws Throwable {  // Compliant
    System.out.println("foo");
    super.finalize();
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();                           // Non-Compliant
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

  void finalize() {
    if (0) {
      super.finalize();
    } else {
      super.finalize();                         // Non-Compliant
    }
  }

  void finalize() {
    try {
      // ...
    } finally {
      super.finalize();                         // Compliant
    }

    int a;
  }

  void finalize() {
    try {
      // ...
    } finally {
      super.finalize();                         // Non-Compliant
      System.out.println();
    }
  }

  void finalize() {
    try {
      // ...
    } catch (Exception e) {
      super.finalize();                         // Non-Compliant
    }
  }
  public void finalize(Object pf, int mode) {

  }
}
