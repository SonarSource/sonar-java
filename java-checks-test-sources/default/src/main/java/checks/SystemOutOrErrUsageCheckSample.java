package checks;

import java.io.PrintStream;

class SystemOutOrErrUsageCheckSample {

  void f() {
    System.out.println(""); // Noncompliant {{Replace this use of System.out by a logger.}}
    System.err.println(""); // Noncompliant {{Replace this use of System.err by a logger.}}
//  ^^^^^^^^^^
    K.out();
    K.err();


    g(System.out); // Noncompliant

    System.arraycopy(null, 0, null, 0, 0);   // Compliant
    java.lang.System.out.println(""); // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^
    java.lang. // Noncompliant
//^[sc=5;ec=17;sl=18;el=19]
      System.out.println("");
  }

  void g(PrintStream stream){

  }

  class K {
    static void out(){};
    static void err(){};
  }

}
