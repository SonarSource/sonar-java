package checks;

import java.util.function.Supplier;

class ExpressionComplexityCheck {
  int a = false ? (true ? (false ? 1 : 0) : 0) : 1;
  int b = false ? (true ? (false ? (true ? 1 : 0) : 0) : 0) : 1;      // Noncompliant [[sc=11;ec=64;effortToFix=1]] {{Reduce the number of conditional operators (4) used in the expression (maximum allowed 3).}}

  boolean c = true || false || true || false || false;                    // Noncompliant [[effortToFix=1]]
  boolean d = true && false && true && false && true && true;             // Noncompliant [[effortToFix=2]] {{Reduce the number of conditional operators (5) used in the expression (maximum allowed 3).}}

  boolean e = true | false | true | false;

  void f() {
    if ((true ? 0 : 1) == 0 || false || true && false && true || false) {  // Noncompliant [[effortToFix=3]] {{Reduce the number of conditional operators (6) used in the expression (maximum allowed 3).}}
    }
  }

  void g() {
    new ExpressionComplexityCheck() {
      boolean a = true && true;
      boolean b = true && true;
      boolean c = true && true;
      boolean d = true && true;
      boolean e = true && true;
    };
  }

  void h() {
    boolean foo = true && true && true &&                             // Noncompliant [[sc=19;ec=11;effortToFix=1]]
      new ExpressionComplexityCheck() {
        boolean a = true && true && true && false && false; // Noncompliant [[effortToFix=1]]
        boolean a2 = true && true && true;
      }.someThing() &&
      true;
  }

  boolean someThing() {
    return true;
  }

  boolean[] foo = new boolean[] {
    true && true && true && true,
    true && true && true && true && true                              // Noncompliant
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
    return new Stuff(true && false, true ? "":"plop", true ? "":"plop", true ? "":"plop"); // Noncompliant
  }


  Runnable lambda() {
    Supplier<Boolean> b1 = false ? () -> true && true : () -> true || false;
    Supplier<Boolean> b2 = false ? () -> true && true && false : () -> true || false; // Noncompliant
    return () -> {
      boolean c = true || false || true;
      c = true || false || true;
      boolean a = true && true && true && false && false; // Noncompliant
      new ExpressionComplexityCheck() {
        boolean a = true && true && true && false && false; // Noncompliant
        boolean a2 = true && true && true;
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

  boolean afterEquals = true || false || true || false || false; // Noncompliant [[effortToFix=1]]
}

class ExpressionComplexity2 {
  @Override
  public boolean equals(Object o) {
    class Local {
      boolean insideLocal = true || false || true || false || false; // Noncompliant [[effortToFix=1]]
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
