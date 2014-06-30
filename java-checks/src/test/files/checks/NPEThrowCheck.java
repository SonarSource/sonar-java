class A {
  void foo() throws NullPointerException {
  }
  void bar() {
    throw new NullPointerException();
  }
  void baz() {
    throw new java.lang.NullPointerException();
  }
  void qix() throws IllegalArgumentException {
    throw new IllegalArgumentException();
  }
}