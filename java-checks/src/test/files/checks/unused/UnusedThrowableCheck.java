class A {
  void foo(int x) {
    if (x < 0) {
      new IllegalArgumentException("x must be nonnegative"); // Noncompliant {{Throw this exception or remove this useless statement.}} [[sc=7;ec=60]]
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

  void testingQuickFixes() {
    new IllegalArgumentException("x must be nonnegative"); // Noncompliant [[sc=5;ec=58;quickfixes=qf1,qf2]]
    // fix@qf1 {{Add "throw"}}
    // edit@qf1 [[sc=5;ec=5]] {{throw }}
    // fix@qf2 {{Remove the statement}}
    // edit@qf2 [[sc=5;ec=59]] {{}}
  }
}
