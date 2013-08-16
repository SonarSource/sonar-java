class A {
  private void f(Throwable e) {
    e.printStackTrace(); // Non-Compliant
    e.printStackTrace(System.out); // Non-Compliant
    e.getMessage(); // Compliant
    a.b.c.d.printStackTrace(); // Non-Compliant
    e.printStackTrace[0]; // Compliant
  }
}
