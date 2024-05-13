package checks;

class SwitchWithTooManyCasesCheckSample {
  void foo() {
    switch (1) { // Noncompliant {{Reduce the number of non-empty switch cases from 35 to at most 30.}}
//  ^^^^^^
      case 1:
        System.out.println("");
      case 2:
        break;
      case 3:
        break;
      case 4:
        break;
      case 5:
        break;
      case 6:
        break;
      case 7:
        break;
      case 8:
        break;
      case 9:
        break;
      case 10:
        break;
      case 11:
        break;
      case 12:
        System.out.println("");
        break;
      case 13:
        break;
      case 14:
        break;
      case 15:
        break;
      case 16:
        break;
      case 17:
        break;
      case 18:
        break;
      case 19:
        break;
      case 20:
        break;
      case 21:
        break;
      case 22:
        break;
      case 23:
        break;
      case 24:
        break;
      case 25:
        break;
      case 26:
        break;
      case 27:
        break;
      case 28:
        break;
      case 29:
        break;
      case 30:
        break;
      case 31:
        break;
      case 32:
        break;
      case 33:
        break;
      case 34:
      case 35:
      case 36:
      case 37:
        System.out.println("");
        break;
      default:
        System.out.println("");
    }
    switch (1) { // Compliant
      case 1:
        System.out.println("");
      case 2:
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
        System.out.println("");
      case 13:
      case 14:
      case 15:
      case 16:
      case 17:
      case 18:
      case 19:
      case 20:
      case 21:
      case 22:
      case 23:
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
      case 30:
      case 31:
      case 32:
      case 33:
      case 34:
        System.out.println("");
      default:
        System.out.println("");
    }
    switch (1) {
      case 1:
      case 2:
      case 3:
        System.out.println("");
      case 4:
      case 5:
      case 6:
        System.out.println("");
    }
  }
}
