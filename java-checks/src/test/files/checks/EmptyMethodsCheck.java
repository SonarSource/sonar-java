class A {
  // Compliant
  public A() {
  }

  // Compliant
  private A() {
  }

  // Compliant
  private <T> A() {
  }

  // Non-Compliant
  private void f() {
  }

  // Compliant
  private void f() {
    /* hmm */
  }

  // Compliant
  private void f() {
    throw new UnsupportedOperationException();
  }

  // Non-Compliant
  private <T> void f() {
  }

  // Non-Compliant
  private int f() {
  }

  // Compliant
  private int f() {
    return 0;
  }

  // Compliant
  private abstract void f();

}
