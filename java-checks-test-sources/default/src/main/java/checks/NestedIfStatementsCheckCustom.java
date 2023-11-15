package checks;

class NestedIfStatementsCheckCustom {
  private void f(int foo, boolean cond) {
    if (false) { // Compliant - 1
    }

    if (false) { // Compliant - 1
    }

    if (false) { // Compliant - 1
    }

    if (false) { // Compliant - 1
    }

    if (false) { // Compliant - 1
      if (true) { // Compliant - 2
      } else {
        if (false) { // Compliant - 3
          if (true) { // Compliant - 4
            if (false) { // Noncompliant {{Refactor this code to not nest more than 4 if/for/while/switch/try statements.}}
            }
          } else if (true) { // Compliant - 4
          } else {
            if (false) { // Noncompliant
            }
          }
        }
      }
    }

    if (false) { // Compliant - 1
    } else if (false) { // Compliant - 1
    } else if (false) { // Compliant - 1
    } else if (false) { // Compliant - 1
    } else if (false) { // Compliant - 1
    }

    if (false) // Compliant - 1
      if (false) // Compliant - 2
        if (false) // Compliant - 3
          if (true) // Compliant - 4
            System.out.println();

    if (false) // Compliant - 1
      if (false) // Compliant - 2
        if (false) // Compliant - 3
          if (false) System.out.println(); // Compliant - 4
          else System.out.println();
        else System.out.println();
      else System.out.println();
    else System.out.println();

    for (int i = 0; i < 0; i++) { // Compliant - 1
      for (Object o: getObjects()) { // Compliant - 2
        while (cond) { // Compliant - 3

          for (int j = 0; i < 0; i++) { // Compliant - 4
          }

          for (Object p: getObjects()) { // Compliant - 4
          }

          while (cond) { // Compliant - 4
          }

          do { // Compliant - 4
          } while (false);

          if (false) { // Compliant
          }

          switch (foo) { // Compliant
          }

          try {  // Compliant
          } catch (Exception e) {
          }
        }
      }
    }
  }

  private Iterable<? extends Object> getObjects() {
    return null;
  }
}
