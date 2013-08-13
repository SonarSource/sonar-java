class A {
  private abstract void f();

  private void f() { // Compliant
  }

  private void f() { // Compliant
    return 0;
  }

  private void f() { // Non-Compliant
    super.f();
  }

  private void f() { // Non-Compliant
    return super.f();
  }

  private void f() { // Compliant
    super.f(0);
  }

  private void f(int a) { // Compliant
    super.f();
  }

  private void f(int a) { // Non-Compliant
    super.f(a);
  }

  private int f(int a) { // Non-Compliant
    return super.f(a);
  }

  private int f(int a) { // Compliant
    super.f(a);
    return a;
  }

  private void f(int a, int b) { // Compliant
    super.f(b, a);
  }

  private void f(int... a) { // Non-Compliant
    super.f(a);
  }

  private void f() { // Compliant
    foo();
  }

  private void f() { // Compliant
    return;
  }

  private <T> void f() { // Non-Compliant
    super.f();
  }

  public A() { // Compliant
    super();
  }

}
