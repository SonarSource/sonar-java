class A {
  private void f() { // Compliant
    if (false) { // Compliant
      { // Non-Compliant
      }
    }

    { // Non-Compliant
      System.out.println();
    }

    while (false) { // Compliant
    }

    do { // Compliant
    } while (false);

    for (int i = 0; i < 42; i++) { // Compliant
    }

    switch (false) {
      case false:
      { // Non-Compliant
      }
    }
  }

  { // Compliant
    { // Non-Compliant
    }
  }

  static { // Compliant
    { // Non-Compliant
    }
  }
}
