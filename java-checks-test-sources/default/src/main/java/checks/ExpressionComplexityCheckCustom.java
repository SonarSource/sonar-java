package checks;

class ExpressionComplexityCheckCustom {
  int a = false ? (true ? (false ? 1 : 0) : 0) : 1;
  int b = false ? (true ? (false ? (true ? 1 : 0) : 0) : 0) : 1;

  boolean c = true || false || true || false || false;
  boolean d = true && false && true && false && true && true; // Noncompliant {{Reduce the number of conditional operators (5) used in the expression (maximum allowed 4).}}

  boolean e = true | false | true | false;

  void f() {
    if ((true ? 0 : 1) == 0 || false || true && false && true || false) { // Noncompliant {{Reduce the number of conditional operators (6) used in the expression (maximum allowed 4).}}
    }
  }

  void g() {
    new Foo() {
      boolean a = true && true;
      boolean b = true && true;
      boolean c = true && true;
      boolean d = true && true;
      boolean e = true && true;
    };
  }

  void h() {
    boolean foo = true && true && true &&
      new ExpressionComplexityCheckCustom() {
        boolean a = true && true && true && false && false;
        boolean a2 = true && true && true;
      }.someThing() &&
      true;
  }

  boolean someThing() {
    return true;
  }

  boolean[] foo = new boolean[] {
    true && true && true && true,
    true && true && true && true && true
  };

  record SD(String applicationName, String serverName, String serverClassPath, String serverArgs, String serverVmArgs) { }
  SD sd;
  String s = "ServerDef[applicationName=" + sd.applicationName() +
      " serverName=" + sd.serverName() +
      " serverClassPath=" + sd.serverClassPath() +
      " serverArgs=" + sd. serverArgs() +
      " serverVmArgs=" + sd.serverVmArgs() +
      "]" ;

  record Stuff(boolean b, String ... s) {}
  Stuff meth(){
    return new Stuff(true && false, true ? "" : "plop", true ? "" : "plop", true ? "" : "plop");
  }
}
