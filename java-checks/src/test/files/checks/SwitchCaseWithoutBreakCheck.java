class A {
  private void f() {
    int myVariable = 0;
    switch (myVariable) {
      case 0:
      case 1: // Noncompliant {{End this switch case with an unconditional break, return or throw statement.}}
        System.out.println();
      case 2: // Compliant
        break;
      case 3: // Compliant
        return;
      case 4: // Compliant
        throw new IllegalStateException();
      case 5: // Noncompliant
        System.out.println();
      default: // Noncompliant
        System.out.println();
      case 6: // Noncompliant
        int a = 0;
      case 8: { // Compliant
        if (false) {
          break;
        } else {
          break;
        }
      }
      case 9: // Compliant
    }

    for (int i = 0; i < 1; i++) {
      switch (myVariable) {
        case 0: // Noncompliant
          continue; // belongs to for loop
        case 1:
          break;
      }

    }

    switch (myVariable) {
      case 0: // Noncompliant
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
              case 0: // Noncompliant
                System.out.println();
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

    switch(myVariable) {

    }
  }
}
