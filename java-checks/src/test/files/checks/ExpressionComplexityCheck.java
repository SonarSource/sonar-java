class A {
  int a = false ? (true ? (false ? 1 : 0) : 0) : 1;                   
  int b = false ? (true ? (false ? (true ? 1 : 0) : 0) : 0) : 1;      // Noncompliant [[sc=11;ec=64;effortToFix=1]] {{Reduce the number of conditional operators (4) used in the expression (maximum allowed 3).}}

  int c = true || false || true || false || false;                    // Noncompliant [[effortToFix=1]]
  int d = true && false && true && false && true && true;             // Noncompliant [[effortToFix=2]] {{Reduce the number of conditional operators (5) used in the expression (maximum allowed 3).}}

  int e = true | false | true | false;                                

  void f() {
    if ((true ? 0 : 1) || false || true && false && true || false) {  // Noncompliant [[effortToFix=3]] {{Reduce the number of conditional operators (6) used in the expression (maximum allowed 3).}}
    }
  }

  void g() {
    new Foo() {                                                       
      int a = true && true;
      int b = true && true;
      int c = true && true;
      int d = true && true;
      int e = true && true;
    };
  }

  void g() {
    boolean foo = true && true && true &&                             // Noncompliant [[sc=19;ec=11;effortToFix=1]]
      new Foo() {                                                     
        int a = true && true && true && false && false;               // Noncompliant [[effortToFix=1]]
        int a2 = true && true && true;
      }.someThing() &&
      true;
  }

  int[] foo = new int[] {                                             
    true && true && true && true,                                     
    true && true && true && true && true                              // Noncompliant
  };
  String s = "ServerDef[applicationName=" + sd.applicationName +
      " serverName=" + sd.serverName +
      " serverClassPath=" + sd.serverClassPath +
      " serverArgs=" + sd. serverArgs +
      " serverVmArgs=" + sd.serverVmArgs +
      "]" ;
  ObjectInstance meth(){
    return new ObjectInstance(true && false, true ? "":"plop", true ? "":"plop", true ? "":"plop"); // Noncompliant
  }

  Runnable lambda() {
    Supplier<Boolean> b1 = false ? () -> true && true : true || false;
    Supplier<Boolean> b2 = false ? () -> true && true && false : true || false; // Noncompliant
    return () -> {
      boolean c = true || false || true;
      c = true || false || true;
      boolean a = true && true && true && false && false; // Noncompliant
      new Foo() {
        int a = true && true && true && false && false;  // Noncompliant
        int a2 = true && true && true;
      }.someThing();
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass() || true || false || false) { // Compliant, the expression is inside equals
      return false;
    }

    return true && true && true && false && false; // Compliant, the expression is inside equals method
  }

  int afterEquals = true || false || true || false || false; // Noncompliant [[effortToFix=1]]

  @Override
  public boolean equals(Object o) {
    class Local {
      int insideLocal = true || false || true || false || false; // Noncompliant [[effortToFix=1]]
      @Override
      public boolean equals(Object o) {
        return true && true && true && false && false; // Compliant, the expression is inside equals method
      }
    }
    return true && true && true && false && false; // Compliant, the expression is inside equals method
  }
}

enum AbbreviationOfDays{
  VALUE;
  public boolean foo()  {
    return true || false || true || false || false; // Noncompliant [[effortToFix=1]]
  }
}
