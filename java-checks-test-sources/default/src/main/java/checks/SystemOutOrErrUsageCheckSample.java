package checks;

import java.io.PrintStream;

class SystemOutOrErrUsageCheckSample {

  void f() {
    System.out.println("");
    System.err.println("");
    K.out();
    K.err();


    g(System.out);

    System.arraycopy(null, 0, null, 0, 0);   // Compliant
    java.lang.System.out.println("");
    java.lang.System.out.println("");
  }

  void g(PrintStream stream){

  }

  class K {
    static void out(){};
    static void err(){};
  }

}
