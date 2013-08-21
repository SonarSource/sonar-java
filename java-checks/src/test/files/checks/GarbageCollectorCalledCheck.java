class A {
  private void f() {
    System.gc(); // Non-Compliant
    foo.gc(); // Compliant
    System.exit(0); // Compliant
    System.gc; // Compliant
    System.gc[0]; // Compliant
    Runtime.getRuntime().gc(); // Non-Compliant
  }
}
