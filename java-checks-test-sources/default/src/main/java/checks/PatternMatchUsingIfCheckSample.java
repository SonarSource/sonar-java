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
    // Compliant: distinct calls to mkExpr() could return distinct values
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

  int goodCompute3(Expr expr) {
    if (expr instanceof Plus plus && plus.lhs.equals(ZERO) && plus.rhs.equals(ZERO)) {
      return 0;
    } else if (expr instanceof Plus) {
      var plus = (Plus) expr; // Compliant for this rule, but reported by S6201
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

  int goodCompute4(Expr expr){
    if (expr instanceof Plus plus && plus.lhs.equals(ZERO) && plus.rhs.equals(ZERO)) {
      return 0;
    } else if (expr instanceof Plus plus) {
      return goodCompute4(plus.lhs) + goodCompute4(plus.rhs);
    } else if (expr instanceof Minus(var l, Expr r) && r.equals(ZERO)) {
      return goodCompute4(l);
    } else if (expr instanceof Minus(var l, Expr r)) {
      return goodCompute4(l) - goodCompute4(r);
    } else if (expr instanceof Const(var i)) {
      return i;
    }
    throw new AssertionError();
  }

  // fix@qf1 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf1 [[sl=+0;el=+12;sc=5;ec=6]] {{switch (expr) {\n      case Plus plus when plus.lhs.equals(ZERO) && plus.rhs.equals(ZERO) -> {\n        return 0;\n      }\n      case Plus plus -> {\n        return badCompute(plus.lhs) + badCompute(plus.rhs);\n      }\n      case Minus(var l, Expr r) when r.equals(ZERO) -> {\n        return badCompute(l);\n      }\n      case Minus(var l, Expr r) -> {\n        return badCompute(l) - badCompute(r);\n      }\n      case Const(var i) -> {\n        return i;\n      }\n      default -> {\n        throw new AssertionError();\n      }\n    }}}
  int badCompute(Expr expr) {
    // Noncompliant@+1 [[sl=+1;el=+1;sc=5;ec=7;quickfixes=qf1]]
    if (expr instanceof Plus plus && plus.lhs.equals(ZERO) && plus.rhs.equals(ZERO)) {
      return 0;
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

  // Compliant: one of the instanceofs is performed on expr2
  int badButAcceptableCompute(Expr expr1, Expr expr2) {
    if (expr1 instanceof Plus plus && plus.lhs.equals(ZERO) && plus.rhs.equals(ZERO)) {
      return 0;
    } else if (expr1 instanceof Plus plus) {
      return badCompute(plus.lhs) + badCompute(plus.rhs);
    } else if (expr2 instanceof Minus(var l, Expr r) && r.equals(ZERO)) {
      return badCompute(l);
    } else if (expr1 instanceof Minus(var l, Expr r)) {
      return badCompute(l) - badCompute(r);
    } else if (expr1 instanceof Const(var i)) {
      return i;
    } else {
      throw new AssertionError();
    }
  }

  private static Expr mkExpr() {
    return new Plus(new Const(10), new Minus(new Const(12), new Const(25)));
  }

  // fix@qf3 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf3 [[sl=+0;el=+6;sc=7;ec=8]] {{switch (x) {\n        case 0, 1 -> {\n          return "binary";\n        }\n        case -1 -> {\n          return "negative";\n        }\n        default -> {\n          return "I don't know!";\n        }\n      }}}
  String badFoo1(int x, boolean b) {
    if (b){
      // Noncompliant@+1 [[sl=+1;el=+1;sc=7;ec=9;quickfixes=qf3]]
      if (x == 0 || x == 1) {
        return "binary";
      } else if (x == -1) {
        return "negative";
      } else {
        return "I don't know!";
      }
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

  // fix@qf5 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf5 [[sl=+0;el=+5;sc=7;ec=22]] {{switch (x) {\n        case -1 -> {\n          return "negative";\n        }\n        case 0 -> {\n          return "zero";\n        }\n        default -> {\n          return "one";\n        }\n      }}}
  String badFoo3(int x) {
    if (x == 0 || x == 1 || x == -1)
      if (x == -1)  // Noncompliant [[sl=+0;el=+0;sc=7;ec=9;quickfixes=qf5]]
        return "negative";
      else if (x == 0)
        return "zero";
      else
        return "one";
    else
      return "Hello world";
  }

  // fix@qf6 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf6 [[sl=+0;el=+5;sc=7;ec=22]] {{switch (x) {\n        case -1 -> {\n          return "negative";\n        }\n        case 2, 0 -> {\n          return "even";\n        }\n        default -> {\n          return "one";\n        }\n      }}}
  String badFoo4(int x) {
    if (0 == x || x == 1 || -1 == x || x == 2)
      if (-1 == x)  // Noncompliant [[sl=+0;el=+0;sc=7;ec=9;quickfixes=qf6]]
        return "negative";
      else if (x == 2 || 0 == x)
        return "even";
      else
        return "one";
    else
      return "Hello world";
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
    // Compliant: guards are only supported for patterns
    if ((x == 0 || x == 1) && y < 10) {
      return "Hello world";
    } else if (x == -1) {
      return "negative";
    } else {
      return "I don't know!";
    }
  }

  String goodFoo3(int x, int y) {
    if (x == 0 || x == y) {
      return "Hello world";
    } else if (x == -1) {
      return "negative";
    } else {
      return "I don't know!";
    }
  }

  String goodFoo4(int x, int y) {
    if (x == 0 || x == 1) {
      return "Hello world";
    } else if (x == y) {
      return "negative";
    } else {
      return "I don't know!";
    }
  }

  String goodFoo5(int x, int y) {
    if (x == 0 || x == 1 || y == 2)
      if (x == -1)
        return "negative";
      else if (x == y)
        return "zero";
      else
        return "one";
    else if (x == 42)
      return "Hello world";
    else
      return "??";
  }

  String goodFoo6(int x, int y) {
    if (x == 0 || x == -y) {
      return "Hello world";
    } else if (x == -1) {
      return "negative";
    } else {
      return "I don't know!";
    }
  }

  String goodFoo7(int x) {
    if (x > 0) {
      return "Hello world";
    } else if (x == -1) {
      return "negative";
    } else {
      return "I don't know!";
    }
  }

  String goodFoo8(int x) {
    if (x > 0 || x == -42) {
      return "Hello world";
    } else if (x == -1) {
      return "negative";
    } else {
      return "I don't know!";
    }
  }

  String goodFoo9(int x) {
    if (x == 0) {
      return "Hello world";
    } else if (x < 0) {
      return "negative";
    } else {
      return "I don't know!";
    }
  }

  String goodFoo10(int x) {
    if (x == 0 || x == 1) {
      return "Hello world";
    } else if (x == -1) {
      return "negative";
    }
    return "I don't know!";
  }

  enum Bar {
    B1, B2, B3, B4, B5
  }

  // fix@qf4 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf4 [[sl=+0;el=+6;sc=5;ec=6]] {{switch (b) {\n      case Bar.B1 -> {\n        return "b1";\n      }\n      case Bar.B2, B3, Bar.B4 -> {\n        return "b234";\n      }\n      default -> {\n        return "b5";\n      }\n    }}}
  static String badBar(Bar b) {
    // Noncompliant@+1 [[sl=+1;el=+1;sc=5;ec=7;quickfixes=qf4]]
    if (b == Bar.B1) {
      return "b1";
    } else if (b == Bar.B2 || b == B3 || b == Bar.B4) {
      return "b234";
    } else {
      return "b5";
    }
  }

  void coverage(int x) {
    if (x == 1 && x*3<10) {
      System.out.println("one");
    } else if (x == 2) {
      System.out.println("two");
    }
  }

  private static final int ONE = 1;

  // fix@qf7 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf7 [[sl=+0;el=+6;sc=5;ec=6]] {{switch (x) {\n      case ONE -> {\n        System.out.println("one");\n      }\n      case 2 -> {\n        System.out.println("two");\n      }\n      default -> {\n        System.out.println("??");\n      }\n    }}}
  void compatingWithConst(int x) {
    // Noncompliant@+1 [[sl=+1;el=+1;sc=5;ec=7;quickfixes=qf7]]
    if (x == ONE) {
      System.out.println("one");
    } else if (x == 2) {
      System.out.println("two");
    } else {
      System.out.println("??");
    }
  }

  // fix@qf8 {{Replace the chain of if/else with a switch expression.}}
  // edit@qf8 [[sl=+0;el=+6;sc=5;ec=6]] {{switch (x) {\n      case 3*3 -> {\n        System.out.println("one");\n      }\n      case 2 -> {\n        System.out.println("two");\n      }\n      default -> {\n        System.out.println("??");\n      }\n    }}}
  void compatingWithSillyMath(int x) {
    // Noncompliant@+1 [[sl=+1;el=+1;sc=5;ec=7;quickfixes=qf8]]
    if (x == 3*3) {
      System.out.println("one");
    } else if (x == 2) {
      System.out.println("two");
    } else {
      System.out.println("??");
    }
  }

}
