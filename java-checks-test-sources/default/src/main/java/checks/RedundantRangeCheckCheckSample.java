package checks;

class RedundantRangeCheckCheckSample {
  void testCompliant(int x, int y) {
    if (x >= 0) {
    }
    if (x >= 0 && y >= 5) {
    }
    if (x >= 0 || x >= 5) {
    }
    if (x == 0 && x == 5) {
    }
    if (x >= 0 && x <= 10) {
    }
    if (x > 0 && x < 10) {
    }
    if ((x >= 0) && (y >= 5)) {
    }
    boolean flag = true;
    if (flag && x > 0) {
    }
    if (x > 0 && flag) {
    }
    if (flag && x > 0 && x < 10) {
    }
  }

  void testNoncompliant(int x) {
    if (x >= 0 && x >= 5) { // Noncompliant {{Remove this redundant range check.}}
//      ^^^^^^
      System.out.println(x);
    }

    if (x >= 5 && x >= 0) { // Noncompliant {{Remove this redundant range check.}}
//                ^^^^^^
      System.out.println(x);
    }

    if (x > 0 && x >= 0) { // Noncompliant {{Remove this redundant range check.}}
//               ^^^^^^
      System.out.println(x);
    }

    if (x >= 0 && x > 0) { // Noncompliant {{Remove this redundant range check.}}
//      ^^^^^^
      System.out.println(x);
    }

    if (x <= 10 && x <= 5) { // Noncompliant {{Remove this redundant range check.}}
//      ^^^^^^^
      System.out.println(x);
    }

    if (x <= 5 && x <= 10) { // Noncompliant {{Remove this redundant range check.}}
//                ^^^^^^^
      System.out.println(x);
    }

    if (x < 10 && x < 5) { // Noncompliant {{Remove this redundant range check.}}
//      ^^^^^^
      System.out.println(x);
    }

    if (x < 5 && x < 10) { // Noncompliant {{Remove this redundant range check.}}
//               ^^^^^^
      System.out.println(x);
    }

    if (x >= 0 && x >= 5 && x >= 10) { // Noncompliant {{Remove this redundant range check.}} {{Remove this redundant range check.}}
      System.out.println(x);
    }

    if ((x >= 0 && x >= 5) && x >= 10) { // Noncompliant {{Remove this redundant range check.}} {{Remove this redundant range check.}}
      System.out.println(x);
    }

    if ((x >= 0) && (x >= 5)) { // Noncompliant {{Remove this redundant range check.}}
      System.out.println(x);
    }
  }

  void testLiteralOnLeft(int x) {
    if (5 < x && 10 < x) { // Noncompliant {{Remove this redundant range check.}}
      System.out.println(x);
    }

    if (0 <= x && 5 <= x) { // Noncompliant {{Remove this redundant range check.}}
      System.out.println(x);
    }

    if (5 < x && x < 10) {
    }
  }

  void testIdenticalComparisons(int x) {
    if (x > 5 && x > 5) { // compliant - S1764 handles identical comparisons
      System.out.println(x);
    }
  }

  void testLongLiterals(int x) {
    if (x >= 0L && x >= 5L) { // Noncompliant {{Remove this redundant range check.}}
      System.out.println(x);
    }
  }

  void testEdgeCases(int x, int y, int min, int max) {
    if (x >= min && x >= max) {
    }
    if (getVal() >= 0 && getVal() >= 5) {
    }
    if (x >= 0 && y >= 0 && x >= 5) { // Noncompliant {{Remove this redundant range check.}}
    }
  }

  void testMixedOperators(int x) {
    if (x > 5 && x >= 3) { // Noncompliant {{Remove this redundant range check.}}
      System.out.println(x);
    }
    if (x >= 5 && x > 3) { // Noncompliant {{Remove this redundant range check.}}
      System.out.println(x);
    }
    if (x < 5 && x <= 8) { // Noncompliant {{Remove this redundant range check.}}
      System.out.println(x);
    }
    if (x <= 5 && x < 8) { // Noncompliant {{Remove this redundant range check.}}
      System.out.println(x);
    }
  }

  void testSideEffects(int size) {
    if (size >= 0 && refresh() && size >= 5) { // compliant - side effects between comparisons
    }
  }

  boolean refresh() {
    return true;
  }

  int getVal() {
    return 0;
  }
}
