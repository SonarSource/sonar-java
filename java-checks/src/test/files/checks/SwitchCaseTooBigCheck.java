class A {
  void f() {
    switch (x) {
      case 0: // Compliant - 1 - total = 5
        System.out.println(); // 2
        System.out.println(); // 3
        System.out.println(); // 4
        System.out.println(); // 5
      case 1: // Non-Compliant - 1 - total = 6
        System.out.println(); // 2
        System.out.println(); // 3
        System.out.println(); // 4
        System.out.println(); // 5
        break; // 6
      case 2: { System.out.println(); // Non-Compliant - 1 - total = 6
      System.out.println(); // 2
      System.out.println(); // 3
      System.out.println(); // 4
      System.out.println(); // 5
      System.out.println(); } // 6
      default: // Non-Compliant - 1 - total = 7
        System.out.println();
        /* foo */

        System.out.println(
        );
        /* tata */
      case 0:
      case 1: // Non-Compliant - 1 - total = 6
        // foo2
        // foo3
        // foo4
        // foo5
        // foo6
    }

    switch (myVariable) {
      case 0:                     // Compliant - 5 lines till following case
        System.out.println("");
        System.out.println("");
        System.out.println("");
        break;
      default:                    // Non-Compliant - 6 lines till switch end
        System.out.println("");
        System.out.println("");
        System.out.println("");
        System.out.println("");
        break;
    }

    switch (myVariable) {
    case 0: System.out.println(); default: System.out.println(); }
  }
}
