package checks;

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

  // $fix@qf1 {{Replace the chain of if/else with a switch expression.}}
  int badCompute(Expr expr) {
    // $Noncompliant@+1 [[sl=+1;el=+1;sc=5;ec=7;quickfixes=qf1]]
    if (expr instanceof Plus plus && plus.rhs.equals(ZERO)) { // Noncompliant
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

  String badFoo(int x) {
    if (x == 0 || x == 1) {  // Noncompliant
      return "binary";
    } else if (x == -1) {
      return "negative";
    } else {
      return "I don't know!";
    }
  }

  String goodFoo(int x) {
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

  enum Bar {
    B1, B2, B3, B4, B5
  }

  String badBar(Bar b) {
    if (b == Bar.B1) {  // Noncompliant
      return "b1";
    } else if (b == Bar.B2 || b == Bar.B3 || b == Bar.B4) {
      return "b234";
    } else {
      return "b5";
    }
  }

}
