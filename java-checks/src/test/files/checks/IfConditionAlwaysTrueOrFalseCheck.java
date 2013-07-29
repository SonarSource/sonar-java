class A {
  private static final boolean FALSE = false;

  void f() {
    if (true) { // Non-Compliant
    } else if (false) { // Non-Compliant
    }

    if (false) { // Non-Compliant
    }

    if (FALSE) { // Compliant
    }

    if (0 + 0 == 1) { // Compliant
    }
  }
}
