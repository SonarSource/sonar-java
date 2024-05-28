package checks;

class NestedTernaryOperatorsCheckSample {

  void qfTest1(int i, int j){

    // fix@qf1 {{Extract this nested ternary operation into an independent statement.}}
    // edit@qf1 [[sl=+0;el=+0;sc=5;ec=59]] {{String s;\n    if (i == j) {\n      s = "equal";\n    } else if (i < j) {\n      s = "less";\n    } else {\n      s = "more";\n    }}}
    // Noncompliant@+1 {{Extract this nested ternary operation into an independent statement.}} [[sl=+1;el=+1;sc=16;ec=58;quickfixes=qf1]]
    String s = i == j ? "equal" : i < j ? "less" : "more";
  }

  void qfTest2(int i, int j){

    // fix@qf2 {{Extract this nested ternary operation into an independent statement.}}
    // edit@qf2 [[sl=+0;el=+0;sc=5;ec=105]] {{String s;\n    if (i == j) {\n      if (i >= 0) {\n        s = "equal, nonnegative";\n      } else {\n        s = "equal, negative";\n      }\n    } else if (i < j) {\n      s = "less";\n    } else {\n      s = "more";\n    }}}
    // Noncompliant@+1 {{Extract this nested ternary operation into an independent statement.}} [[sl=+1;el=+1;sc=16;ec=105;quickfixes=qf2]]
    String s = i == j ? (i >= 0 ? "equal, nonnegative" : "equal, negative") : (i < j ? "less" : "more");
  }

}
