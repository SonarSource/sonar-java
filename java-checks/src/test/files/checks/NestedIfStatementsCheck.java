class A {
  private void f() {
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
          if (true) { // Non-Compliant - 4
            if (false) { // Compliant - 5
            }
          } else if (true) { // Compliant - 4
          } else {
            if (false) { // Compliant - 5
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
          if (true) // Non-Compliant - 4
            System.out.println();

    if (false) // Compliant - 1
      if (false) // Compliant - 2
        if (false) // Compliant - 3
          if (false) System.out.println(); // Non-Compliant - 4
          else System.out.println();
        else System.out.println();
      else System.out.println();
    else System.out.println();
  }
}
