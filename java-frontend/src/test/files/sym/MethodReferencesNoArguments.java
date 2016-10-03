class A {
  D d = new D();

  void foo(java.util.Collection<C> cs, java.util.Collection<D> ds) {
    cs.stream()
      .filter(B::isFalse)
      .filter(C::isTrue)
      .map(C::up);

    ds.stream()
      .filter(d::bool)
      .filter(this.d::bool)
      .filter(getD()::bool)
      .filter(D::bool)
      .filter(A.D::bool);
  }

  static class B {
    private boolean isFalse() { return false; }
  }

  static class C extends B {
    private boolean isTrue() { return true; }
    private B up() { return this; }
  }

  static class D {
    boolean bool(D d) { return true; }
    boolean bool() { return true; }
  }

  D getD() {  return d; }
}
