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

  @SomeCrazyAnnotation // Compliant
  private void f() {
    super.f();
  }

  @Override
  public void f() { // Non-Compliant
    super.f();
  }

  @Override
  @SomeCrazyAnnotation
  private void f() { // Compliant
    super.f();
  }

  @SomeCrazyAnnotation
  @Override
  private void f() { // Compliant
    super.f();
  }

  @SomeCrazyAnnotation
  private <T> void f() { // Compliant
    super.f();
  }

  @foo.Deprecated
  private <T> void f() { // Compliant
    super.f();
  }

  @Override
  @Override
  private <T> void f() { // Non-Compliant
    super.f();
  }


  @Override
  public boolean equals(Object obj) { //non compliant, equals is not final
    return super.equals(obj);
  }

  @Override
  public int hashCode() {//non compliant, hashCode is not final
    return super.hashCode();
  }

  @Override
  public String toString() {//non compliant, string is not final
    return super.toString();
  }

  @Override
  public final boolean equals(Object obj) { //Compliant, equals is final
    return super.equals(obj);
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public final String toString() {
    return super.toString();
  }
}
