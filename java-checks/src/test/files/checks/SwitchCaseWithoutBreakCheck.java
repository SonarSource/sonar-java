class A {
  private void f() {
    switch (myVariable) {
      case 0:
      case 1: // Non-Compliant
        System.out.println();
      case 2: // Compliant
        break;
      case 3: // Compliant
        return;
      case 4: // Compliant
        throw new IllegalStateException();
      case 5: // Non-Compliant
        System.out.println();
      default: // Non-Compliant
        System.out.println();
      case 6: // Non-Compliant
        int a = 0;
      case 7: // Compliant
        continue;
      case 8: { // Compliant
        if (false) {
          break;
        } else {
          break;
        }
      }
      case 9: // Compliant
    }

    switch (myVariable) {
      case 0: // Non Compliant
        switch (myVariable) {
          case 0:
          case 1: // Noncompliant
            System.out.println();
            switch (myVariable){
              case 0:
              case 1:
                break;
            }
          case 2: // Compliant
            break;
        }
        System.out.println();
      case 1: // Compliant
        switch (myVariable) {
          case 0:
          case 1: // Compliant
            System.out.println();
            switch (myVariable){
              case 0:
                System.out.println(); // Non Compliant
              case 1:
                break;
            }
            break;
          case 2: // Compliant
            break;
        }
        break;
      case 2: // Compliant
    }
  }
}
