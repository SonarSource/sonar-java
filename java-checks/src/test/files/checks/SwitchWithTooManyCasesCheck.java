class A{
  void foo() {
    switch (1) { // Noncompliant [[sc=5;ec=11]] {{Reduce the number of switch cases from 35 to at most 30.}}
      case 1:
        System.out.println("");
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
        System.out.println("");
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
      case 1:
        System.out.println("");
      default:
        System.out.println("");
    }
    switch (1) {
      case 1:
      case 1:
      case 1:
        System.out.println("");
      case 1:
      case 1:
      case 1:
        System.out.println("");
    }
  }
}
