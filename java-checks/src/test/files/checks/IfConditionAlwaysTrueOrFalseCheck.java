class A {
  private static final boolean FALSE = false;

  void f() {
    if (true) { // Noncompliant [[sc=9;ec=13]] {{Remove this if statement.}}
    } else if (false) { // Noncompliant
    }

    if (false) { // Noncompliant
    }

    if (FALSE) {
    }

    if (0 + 0 == 1) {
    }
  }
}
