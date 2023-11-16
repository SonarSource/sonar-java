package checks;

class NestedTernaryOperatorsCheck {
  void foo(boolean b) {
    String res;
    res = b ? "yo" : "lo";
    res = (b ? b : !b) ? "yo" : "lo"; // Noncompliant [[sc=12;ec=22]] {{Extract this nested ternary operation into an independent statement.}}
    res = b ? b ? "yo" : "lo" : "yolo"; // Noncompliant [[sc=15;ec=30]]
    res = b ? "yo" : b ? "lo" : "yolo"; // Noncompliant [[sc=22;ec=39]]
    res = b ? "yo" : "lo" + (b ? "ba" : "lo"); // Noncompliant [[sc=30;ec=45]]

    NestedTernaryOperatorsCheck a = b ? new NestedTernaryOperatorsCheck() {
      @Override
      void foo(boolean arg0) {
        String res = b ? "yo" : "lo"; // Compliant - not really nested
        res = b ? "yo" : b ? "lo" : "yolo"; // Noncompliant
      }
    } : new NestedTernaryOperatorsCheck();
  }
}
