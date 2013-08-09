class A {
  int foo = 0;
  int enum = 0; // Non-Compliant

  public void f(
      int a,
      int enum) { // Non-Compliant

  }
}

enum B { // Compliant
  ;
}
