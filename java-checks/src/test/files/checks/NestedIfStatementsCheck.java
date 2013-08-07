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
          } else {
            if (false) { // Compliant - 5
            }
          }
        }
      }
    }
  }
}
