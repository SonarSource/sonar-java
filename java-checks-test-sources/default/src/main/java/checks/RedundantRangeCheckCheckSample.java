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
  }

  void testNoncompliant(int x) {
    if (x >= 0 && x >= 5) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
      System.out.println(x);
    }

    if (x >= 5 && x >= 0) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
      System.out.println(x);
    }

    if (x > 0 && x >= 0) { // Noncompliant {{Remove this redundant range check; it is implied by the "x > 0" check.}}
      System.out.println(x);
    }

    if (x >= 0 && x > 0) { // Noncompliant {{Remove this redundant range check; it is implied by the "x > 0" check.}}
      System.out.println(x);
    }

    if (x <= 10 && x <= 5) { // Noncompliant {{Remove this redundant range check; it is implied by the "x <= 5" check.}}
      System.out.println(x);
    }

    if (x <= 5 && x <= 10) { // Noncompliant {{Remove this redundant range check; it is implied by the "x <= 5" check.}}
      System.out.println(x);
    }

    if (x < 10 && x < 5) { // Noncompliant {{Remove this redundant range check; it is implied by the "x < 5" check.}}
      System.out.println(x);
    }

    if (x < 5 && x < 10) { // Noncompliant {{Remove this redundant range check; it is implied by the "x < 5" check.}}
      System.out.println(x);
    }

    if (x >= 0 && x >= 5 && x >= 10) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 10" check.}} {{Remove this redundant range check; it is implied by the "x >= 5" check.}} {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
      System.out.println(x);
    }

    if ((x >= 0 && x >= 5) && x >= 10) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 10" check.}} {{Remove this redundant range check; it is implied by the "x >= 5" check.}} {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
      System.out.println(x);
    }

    if ((x >= 0) && (x >= 5)) { // Noncompliant {{Remove this redundant range check; it is implied by the "x >= 5" check.}}
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

  int getVal() {
    return 0;
  }
}
