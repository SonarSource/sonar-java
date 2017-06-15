class A {
  public void test() {
    if (a > 0) {
      int b = x();
      if (b != 0) {
        call((b == 0) ? 0 : b - 1); // Noncompliant
      }
    }
  }
}
