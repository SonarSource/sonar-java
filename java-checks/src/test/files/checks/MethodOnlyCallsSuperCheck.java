class A {
  private abstract void f();

  private void f() {
  }

  private void f() {
    return 0;
  }

  private void f() { // Noncompliant {{Remove this method to simply inherit it.}}
    super.f();
  }

  private void f() { // Noncompliant
    return super.f();
  }

  private void f() {
    super.f(0);
  }

  private void f(int a) {
    super.f();
  }

  private void f(int a) { // Noncompliant
    super.f(a);
  }

  private int f(int a) { // Noncompliant
    return super.f(a);
  }

  private int f(int a) {
    super.f(a);
    return a;
  }

  private void f(int a, int b) {
    super.f(b, a);
  }

  private void f(int... a) { // Noncompliant
    super.f(a);
  }

  private void f() {
    foo();
  }

  private void f() {
    return;
  }

  private <T> void f() { // Noncompliant
    super.f();
  }

  public A() {
    super();
  }

  @SomeCrazyAnnotation
  private void f() {
    super.f();
  }

  @Override
  public void f() { // Noncompliant
    super.f();
  }

  @Override
  @SomeCrazyAnnotation
  private void f() {
    super.f();
  }

  @SomeCrazyAnnotation
  @Override
  private void f() {
    super.f();
  }

  @SomeCrazyAnnotation
  private <T> void f() {
    super.f();
  }

  @foo.Deprecated
  private <T> void f() {
    super.f();
  }

  @Override
  @Override
  private <T> void f() { // Noncompliant
    super.f();
  }


  @Override
  public boolean equals(Object obj) { // Noncompliant
    return super.equals(obj);
  }

  @Override
  public int hashCode() { // Noncompliant
    return super.hashCode();
  }

  @Override
  public String toString() { // Noncompliant
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
