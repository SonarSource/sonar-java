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
      case "a": { // Compliant
      }
      case "b": { // Compliant
        break;
      }
      case "c": { // Compliant
        doSomething();
      }
      case "d": { // Compliant
        doSomething();
        break;
      }
      case "e": { // Compliant
        doSomething();
        doSomething();
      }
      case "f": { // Compliant
        doSomething();
        doSomething();
        break;
      }
      case "0":
    }

    switch (s) {
      case "a": {
        { // Noncompliant
          System.out.println();
        }
      }
      case "b":
        { // Noncompliant
          doSomething();
        }
        { // Noncompliant
          doSomething();
          break;
        }
      case "b2":
        { // Noncompliant
          doSomething();
        }
        { // Noncompliant
          doSomething();
        }
        break;
      case "c":
        { // Compliant
          doSomething();
        }
        break;
      case "d":
        { // Compliant
          doSomething();
        }
        return;
      case "e":
        { // Noncompliant
          doSomething();
        }
        doSomething();
    }
  }


  private void doSomething() {
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
