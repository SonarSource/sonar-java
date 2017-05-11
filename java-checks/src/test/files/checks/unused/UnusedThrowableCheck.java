class A {
  void foo(int x) {
    if (x < 0) {
      new IllegalArgumentException("x must be nonnegative"); // Noncompliant {{Throw this exception or remove this useless statement}} [[sc=7;ec=60]]
    }
    if (x < 0) {
      throw new IllegalArgumentException("x must be nonnegative");
    }
    new A();
    Throwable t = new IllegalArgumentException("x must be nonnegative");
    if (x < 0) {
      throw t;
    }
  }
}
