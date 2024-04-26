package checks;

class NestedTernaryOperatorsCheckSample {
  void foo(boolean b) {
    String res;
    res = b ? "yo" : "lo";
    res = (b ? b : !b) ? "yo" : "lo"; // Noncompliant {{Extract this nested ternary operation into an independent statement.}}
//         ^^^^^^^^^^
    res = b ? b ? "yo" : "lo" : "yolo"; // Noncompliant
//            ^^^^^^^^^^^^^^^
    res = b ? "yo" : b ? "lo" : "yolo"; // Noncompliant
//                   ^^^^^^^^^^^^^^^^^
    res = b ? "yo" : "lo" + (b ? "ba" : "lo"); // Noncompliant
//                           ^^^^^^^^^^^^^^^

    NestedTernaryOperatorsCheckSample a = b ? new NestedTernaryOperatorsCheckSample() {
      @Override
      void foo(boolean arg0) {
        String res = b ? "yo" : "lo"; // Compliant - not really nested
        res = b ? "yo" : b ? "lo" : "yolo"; // Noncompliant
      }
    } : new NestedTernaryOperatorsCheckSample();
  }
}
