package checks;

class SwitchCaseTooBigCheckSample {
  void f(int myVariable) {
    int i = switch (myVariable) {
      case 0:
        System.out.println(); // 1
        System.out.println(); // 2
        System.out.println(); // 3
        System.out.println(); // 4
        yield 1; // 5
      case 1: // Noncompliant [[sc=7;ec=14]] {{Reduce this switch case number of lines from 7 to at most 5, for example by extracting code into methods.}}
        System.out.println(); // 1
        System.out.println(); // 2
        System.out.println(); // 3
        System.out.println(); // 4
        System.out.println(); // 5
        System.out.println(); // 6
        yield 1;              // 7
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
        yield 2;                 // 6
    };

    int j = switch (myVariable) {
      case 0 ->  { System.out.println();  // Noncompliant 1
        System.out.println();             // 2
        System.out.println();             // 3
        System.out.println();             // 4
        System.out.println();             // 5
        yield 1;}                         // 6
      default -> 3;
    };

    int k = switch (myVariable) {
      case 0:
        System.out.println("");  // 1
        System.out.println("");  // 2
        System.out.println("");  // 3
        yield 1;                 // 4
      case 1:
        yield 2;
      default:
        yield 3;
    };


  }
}
