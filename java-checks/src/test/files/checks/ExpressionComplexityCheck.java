class A {
  int a = false ? (true ? (false ? 1 : 0) : 0) : 1;                   // Compliant
  int b = false ? (true ? (false ? (true ? 1 : 0) : 0) : 0) : 1;      // Non-Compliant

  int c = true || false || true || false || false;                    // Non-Compliant
  int d = true && false && true && false && true && true;             // Non-Compliant

  int e = true | false | true | false;                                // Compliant

  void f() {
    if ((true ? 0 : 1) || false || true && false && true || false) {  // Non-Compliant
    }
  }

  void g() {
    new Foo() {                                                       // Compliant
      int a = true && true;
      int b = true && true;
      int c = true && true;
      int d = true && true;
      int e = true && true;
    };
  }

  void g() {
    boolean foo = true && true && true &&                             // Non-Compliant
      new Foo() {                                                     // Compliant
        int a = true && true && true && false && false;               // Non-Compliant
        int a = true && true && true;                                 // Compliant
      }.someThing() &&
      true;
  }

  int[] foo = new int[] {                                             // Compliant
    true && true && true && true,                                     // Compliant
    true && true && true && true && true                              // Non-Compliant
  };
  String s = "ServerDef[applicationName=" + sd.applicationName +
      " serverName=" + sd.serverName +
      " serverClassPath=" + sd.serverClassPath +
      " serverArgs=" + sd. serverArgs +
      " serverVmArgs=" + sd.serverVmArgs +
      "]" ;
  ObjectInstance meth(){
    return new ObjectInstance(true && false, true ? "":"plop", true ? "":"plop", true ? "":"plop");
  }
}
