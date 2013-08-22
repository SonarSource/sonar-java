class A {
  private void f() { // Compliant
    if (false) { // Compliant
    }

    { // Non-Compliant
      System.out.println();
    }

    if (false) { // Compliant
      { // Non-Compliant
      }
    }

    while (false) { // Compliant
    }

    do { // Compliant
    } while (false);

    for (int i = 0; i < 42; i++) { // Compliant
    }

    int a;
  }

  { // Compliant
  }
}
