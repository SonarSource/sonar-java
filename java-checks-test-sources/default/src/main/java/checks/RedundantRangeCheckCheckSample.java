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
    if (x >= 0 && x >= 5) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
//      ^^^^^^
      System.out.println(x);
    }

    if (x >= 5 && x >= 0) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
//                ^^^^^^
      System.out.println(x);
    }

    if (x > 0 && x >= 0) { // Noncompliant {{Remove this redundant range check; it is implied by the "x > 0" check.}}
//               ^^^^^^
      System.out.println(x);
    }

    if (x >= 0 && x > 0) { // Noncompliant {{Remove this redundant range check; it is implied by the "x > 0" check.}}
//      ^^^^^^
      System.out.println(x);
    }

    if (x <= 10 && x <= 5) { // Noncompliant {{Remove this redundant range check; it is implied by the "x <= 5" check.}}
//      ^^^^^^^
      System.out.println(x);
    }

    if (x <= 5 && x <= 10) { // Noncompliant {{Remove this redundant range check; it is implied by the "x <= 5" check.}}
//                ^^^^^^^
      System.out.println(x);
    }

    if (x < 10 && x < 5) { // Noncompliant {{Remove this redundant range check; it is implied by the "x < 5" check.}}
//      ^^^^^^
      System.out.println(x);
    }

    if (x < 5 && x < 10) { // Noncompliant {{Remove this redundant range check; it is implied by the "x < 5" check.}}
//               ^^^^^^
      System.out.println(x);
    }

    if (x >= 0 && x >= 5 && x >= 10) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}} {{Remove this redundant range check; it is implied by the "x >= 10" check.}}
      System.out.println(x);
    }

    if ((x >= 0 && x >= 5) && x >= 10) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}} {{Remove this redundant range check; it is implied by the "x >= 10" check.}}
      System.out.println(x);
    }

    if ((x >= 0) && (x >= 5)) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
      System.out.println(x);
    }
  }

  void testLiteralOnLeft(int x) {
    if (5 < x && 10 < x) { // Noncompliant {{Remove this redundant range check; it is implied by the "x > 10" check.}}
      System.out.println(x);
    }

    if (0 <= x && 5 <= x) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
      System.out.println(x);
    }

    if (5 < x && x < 10) {
    }
  }

  void testIdenticalComparisons(int x) {
    if (x > 5 && x > 5) { // Noncompliant {{Remove this redundant range check; it is implied by the "x > 5" check.}}
      System.out.println(x);
    }
  }

  void testLongLiterals(int x) {
    if (x >= 0L && x >= 5L) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
      System.out.println(x);
    }
  }

  void testEdgeCases(int x, int y, int min, int max) {
    if (x >= min && x >= max) {
    }
    if (getVal() >= 0 && getVal() >= 5) {
    }
    if (x >= 0 && y >= 0 && x >= 5) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
    }
  }

  void testMixedOperators(int x) {
    if (x > 5 && x >= 3) { // Noncompliant {{Remove this redundant range check; it is implied by the "x > 5" check.}}
      System.out.println(x);
    }
    if (x >= 5 && x > 3) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
      System.out.println(x);
    }
    if (x < 5 && x <= 8) { // Noncompliant {{Remove this redundant range check; it is implied by the "x < 5" check.}}
      System.out.println(x);
    }
    if (x <= 5 && x < 8) { // Noncompliant {{Remove this redundant range check; it is implied by the "x <= 5" check.}}
      System.out.println(x);
    }
  }

  int getVal() {
    return 0;
  }
}
