class A {
  private void f() {
    if (false) {
      { // Noncompliant [[sc=7;ec=8]] {{Extract this nested code block into a method.}}
      }
    }

    { // Noncompliant
      System.out.println();
    }

    while (false) {
    }

    do {
    } while (false);

    for (int i = 0; i < 42; i++) {
    }

    switch (false) {
      case false:
      { // Noncompliant
      }
    }
  }

  {
    { // Noncompliant
    }
  }

  static {
    { // Noncompliant
    }
  }
}
