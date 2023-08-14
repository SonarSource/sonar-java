package checks;

import java.io.PrintStream;

class SystemOutOrErrUsageCheck {

  void f() {
    System.out.println("");                  // Noncompliant {{Replace this use of System.out by a logger.}}
    System.err.println("");                  // Noncompliant [[sc=5;ec=15]] {{Replace this use of System.err by a logger.}}
    K.out();
    K.err();


    g(System.out);                           // Noncompliant

    System.arraycopy(null, 0, null, 0, 0);   // Compliant
    java.lang.System.out.println("");        // Noncompliant [[sc=5;ec=25]]
    java.lang.                               // Noncompliant [[sc=5;el=+1;ec=17]]
      System.out.println("");
  }

  void g(PrintStream stream){

  }

  class K {
    static void out(){};
    static void err(){};
  }

}
