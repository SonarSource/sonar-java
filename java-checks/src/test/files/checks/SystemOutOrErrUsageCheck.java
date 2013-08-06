class A {

  void f() {
    System.out.println("");                  // Non-Compliant
    System.err.println("");                  // Non-Compliant

    f(System.out);                           // Non-Compliant

    System.arraycopy(null, 0, null, 0, 0);   // Compliant
  }

}
