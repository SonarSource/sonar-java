class A {
  public void finalize() { // Compliant
  }

  @Override
  public void finalize() { // Compliant
  }

  public void finalize(int a) { // Non-Compliant
  }

  private void finalize(int a, int b) { // Non-Compliant
  }

  private int finalize() { // Compliant
    return 0;
  }

  private int finalize(int a) { // Non-Compliant
    return a;
  }

  private void foo() { // Compliant
  }

  private int foo(int a) { // Compliant
    return a;
  }
}
