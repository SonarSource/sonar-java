class A {
  void f() {
    if (false) { // Compliant
    }

    if (false) { // Compliant
    } else {
    }

    if (false) { // Compliant
      if (false) { // Non-Compliant
      }
    }

    if (false) { // Compliant
      if (false) { // Compliant
      }
      System.out.println();
    }

    if (false) { // Compliant
      int a;
      if (a) { // Compliant
      }
    }

    if (false) { // Compliant
      if (false) { // Compliant
      }
    } else {
    }

    if (false) { // Compliant
      if (false) { // Compliant
      } else {
      }
    }

    if (false) { // Compliant
    } else if (false) { // Compliant
      if (false) { // Non-Compliant
      }
    }

    if (false) // Compliant
      if (true) { // Non-Compliant
    }

    if (false) { // Compliant
      while (true) {
        if (true) { // Compliant
        }
      }

      while (true)
        if(true) { // Compliant
      }
    }
  }

  {
    if (false) { // Compliant
    }
      if (false) {
        switch ("SELECT") {
          case "SELECT":
            if ("SELECT".equals(token.getValue())) { // Compliant
            }
            break;
          }
       }
  }
}
