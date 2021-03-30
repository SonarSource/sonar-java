package checks;

class NestedBlocksCheck {
  private void f(String s, boolean b) {
    if (false) {
      { // Noncompliant [[sc=7;ec=8]] {{Extract this nested code block into a method.}}
      }
    }

    { // Noncompliant
      System.out.println();
    }

    while (b) {
    }

    do {
    } while (false);

    for (int i = 0; i < 42; i++) {
    }

    switch (s) {
      case "false":
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
