package checks;

class SwitchCaseTooBigCheck {
  void f(int myVariable) {
    switch (myVariable) {
      case 0:
        System.out.println(); // 1
        System.out.println(); // 2
        System.out.println(); // 3
        System.out.println(); // 4
        System.out.println(); // 5
      case 1: // Noncompliant [[sc=7;ec=14]] {{Reduce this switch case number of lines from 7 to at most 5, for example by extracting code into methods.}}
        System.out.println(); // 1
        System.out.println(); // 2
        System.out.println(); // 3
        System.out.println(); // 4
        System.out.println(); // 5
        System.out.println(); // 6
        break;                // 7
      case 2: { System.out.println();   // Noncompliant 1
        System.out.println();           // 2
        System.out.println();           // 3
        System.out.println();           // 4
        System.out.println();           // 5
        System.out.println(); }         // 6
      case 3: // compliant : empty spaces and comments do not count in loc
        System.out.println();           // 1
                                /* foo */                       // 2

        System.out.println(             // 4
        );                              // 5
                                /* tata */                      // 6
      case 4: // 1
      case 5: // Compliant: only comments 1
        // 2
        // 3
        // 4
        // 5
        // 6
      case 6:


      case 7: // Noncompliant {{Reduce this switch case number of lines from 6 to at most 5, for example by extracting code into methods.}}
        // my empty comment  (does not count)
        System.out.println();   // 1
        System.out.println();   // 2
        System.out.println();   // 3
        System.out.println();   // 4
        System.out.println();   // 5
        System.out.println();   // 6
      default: // Noncompliant
        System.out.println("");  // 1
        System.out.println("");  // 2
        System.out.println("");  // 3
        System.out.println("");  // 4
        System.out.println("");  // 5
        break;                   // 6
    }

    switch (myVariable) {
      case 0:
        System.out.println("");  // 1
        System.out.println("");  // 2
        System.out.println("");  // 3
        break;                   // 4
      case 1:


    }

    switch (myVariable) {
      case 0:
        System.out.println();
      default:
        System.out.println();
    }

    switch (myVariable) {
      case 0: // foo
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        break;
      case 1:
        System.out.println();
    }

  }
}
