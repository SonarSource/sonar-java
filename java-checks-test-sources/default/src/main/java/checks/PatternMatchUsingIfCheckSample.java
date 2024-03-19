package checks;

import static checks.PatternMatchUsingIfCheckSample.Bar.B3;

public class PatternMatchUsingIfCheckSample {

  private static final Const ZERO = new Const(0);

  sealed interface Expr permits Plus, Minus, Const {
  }

  record Plus(Expr lhs, Expr rhs) implements Expr {
  }

  record Minus(Expr lhs, Expr rhs) implements Expr {
  }

  record Const(int value) implements Expr {
  }

  int goodCompute1(Expr expr) {
    switch (expr) {
      case Plus plus when plus.rhs.equals(ZERO) -> {
        return goodCompute1(plus.lhs);
      }
      case Plus plus -> {
        return goodCompute1(plus.lhs) + goodCompute1(plus.rhs);
      }
      case Minus(var l, Expr r) when r.equals(ZERO) -> {
        return goodCompute1(l);
      }
      case Minus(var l, Expr r) -> {
        return goodCompute1(l) - goodCompute1(r);
      }
      case Const(var i) -> {
        return i;
      }
    }
  }

  int goodCompute2() {
    if (mkExpr() instanceof Plus plus && plus.rhs.equals(ZERO)) {
      return goodCompute1(plus.lhs);
    } else if (mkExpr() instanceof Plus plus) {
      return goodCompute1(plus.lhs) + goodCompute1(plus.rhs);
    } else if (mkExpr() instanceof Minus(var l, Expr r) && r.equals(ZERO)) {
      return goodCompute1(l);
    } else if (mkExpr() instanceof Minus(var l, Expr r)) {
      return goodCompute1(l) - goodCompute1(r);
    } else if (mkExpr() instanceof Const(var i)) {
      return i;
    } else {
      throw new AssertionError();
    }
  }

  // fix@qf1 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf1 [[sl=+0;el=+12;sc=5;ec=6]] {{switch (expr) {\n      case Plus plus when plus.rhs.equals(ZERO) -> {\n        return badCompute(plus.lhs);\n      }\n      case Plus plus -> {\n        return badCompute(plus.lhs) + badCompute(plus.rhs);\n      }\n      case Minus(var l, Expr r) when r.equals(ZERO) -> {\n        return badCompute(l);\n      }\n      case Minus(var l, Expr r) -> {\n        return badCompute(l) - badCompute(r);\n      }\n      case Const(var i) -> {\n        return i;\n      }\n      default -> {\n        throw new AssertionError();\n      }\n    }}}
  int badCompute(Expr expr) {
    // Noncompliant@+1 [[sl=+1;el=+1;sc=5;ec=7;quickfixes=qf1]]
    if (expr instanceof Plus plus && plus.rhs.equals(ZERO)) {
      return badCompute(plus.lhs);
    } else if (expr instanceof Plus plus) {
      return badCompute(plus.lhs) + badCompute(plus.rhs);
    } else if (expr instanceof Minus(var l, Expr r) && r.equals(ZERO)) {
      return badCompute(l);
    } else if (expr instanceof Minus(var l, Expr r)) {
      return badCompute(l) - badCompute(r);
    } else if (expr instanceof Const(var i)) {
      return i;
    } else {
      throw new AssertionError();
    }
  }

  private static Expr mkExpr() {
    return new Plus(new Const(10), new Minus(new Const(12), new Const(25)));
  }

  // fix@qf3 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf3 [[sl=+0;el=+4;sc=7;ec=8]] {{switch (x) {\n        case 0, 1 -> {\n          return "binary";\n        }\n        case -1 -> {\n          return "negative";\n        }\n      }}}
  String badFoo1(int x, boolean b) {
    if (b){
      // Noncompliant@+1 [[sl=+1;el=+1;sc=7;ec=9;quickfixes=qf3]]
      if (x == 0 || x == 1) {
        return "binary";
      } else if (x == -1) {
        return "negative";
      }
      return "I don't know!";
    }
    return "?";
  }

  // fix@qf2 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf2 [[sl=+0;el=+6;sc=5;ec=6]] {{switch (x) {\n      case 0, 1 -> {\n        return "Hello world";\n      }\n      case -1 -> {\n        return "negative";\n      }\n      default -> {\n        return "I don't know!";\n      }\n    }}}
  String badFoo2(int x) {
    // Noncompliant@+1 [[sl=+1;el=+1;sc=5;ec=7;quickfixes=qf2]]
    if (x == 0 || x == 1) {
      return "Hello world";
    } else if (x == -1) {
      return "negative";
    } else {
      return "I don't know!";
    }
  }

  String goodFoo1(int x) {
    switch (x) {
      case 0, 1 -> {
        return "binary";
      }
      case -1 -> {
        return "negative";
      }
      default -> {
        return "I don't know!";
      }
    }
  }

  String goodFoo2(int x, int y){
    if ((x == 0 || x == 1) && y < 10) {
      return "Hello world";
    } else if (x == -1) {
      return "negative";
    } else {
      return "I don't know!";
    }
  }

  enum Bar {
    B1, B2, B3, B4, B5
  }

  // fix@qf4 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf4 [[sl=+0;el=+6;sc=5;ec=6]] {{switch (b) {\n      case Bar.B1 -> {\n        return "b1";\n      }\n      case Bar.B2, B3, Bar.B4 -> {\n        return "b234";\n      }\n      default -> {\n        return "b5";\n      }\n    }}}
  String badBar(Bar b) {
    // Noncompliant@+1 [[sl=+1;el=+1;sc=5;ec=7;quickfixes=qf4]]
    if (b == Bar.B1) {
      return "b1";
    } else if (b == Bar.B2 || b == B3 || b == Bar.B4) {
      return "b234";
    } else {
      return "b5";
    }
  }

}
