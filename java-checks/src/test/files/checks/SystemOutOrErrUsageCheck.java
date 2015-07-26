class A {

  void f() {
    System.out.println("");                  // Noncompliant {{Replace this usage of System.out or System.err by a logger.}}
    System.err.println("");                  // Noncompliant {{Replace this usage of System.out or System.err by a logger.}}

    f(System.out);                           // Noncompliant {{Replace this usage of System.out or System.err by a logger.}}

    System.arraycopy(null, 0, null, 0, 0);   // Compliant
    java.lang.System.out.println("");        // Noncompliant {{Replace this usage of System.out or System.err by a logger.}}
  }

}
