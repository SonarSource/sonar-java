package checks;

class SwitchCaseTooBigCheckCustom {
  void f(int myVariable) {
    switch (myVariable) {
      case 0:
        System.out.println(); // 1
        System.out.println(); // 2
        System.out.println(); // 3
        System.out.println(); // 4
        System.out.println(); // 5
      case 1: // Noncompliant {{Reduce this switch case number of lines from 7 to at most 6, for example by extracting code into methods.}}
        System.out.println(); // 1
        System.out.println(); // 2
        System.out.println(); // 3
        System.out.println(); // 4
        System.out.println(); // 5
        System.out.println(); // 6
        break;                // 7
      case 2: { System.out.println();   // 1
        System.out.println();           // 2
        System.out.println();           // 3
        System.out.println();           // 4
        System.out.println();           // 5
        System.out.println(); }         // 6
      case 3:
        System.out.println();           // 1
                                /* foo */                       // 2

        System.out.println(             // 4
        );                              // 5
                                /* tata */                      // 6
      case 4: // 1
      case 5: // 1
        // 2
        // 3
        // 4
        // 5
        // 6
      case 6:


      case 7:
        // my empty comment     // 1
        System.out.println();   // 2
        System.out.println();   // 3
        System.out.println();   // 4
        System.out.println();   // 5
        System.out.println();   // 6
      default:
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
