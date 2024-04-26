package checks;

class SwitchWithTooManyCasesCheckCustom {
  void foo() {
    switch (1) { // Noncompliant {{Reduce the number of non-empty switch cases from 35 to at most 5.}}
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
    switch (1) { // Noncompliant
      case 1:
//  ^^^<
        break;
      case 2:
//  ^^^<
        break;
      case 3:
//  ^^^<
        System.out.println("");
      case 4:
//  ^^^<
        break;
      case 5:
//  ^^^<
        break;
      case 6:
//  ^^^<
      case 7:
        System.out.println("");
    }
  }
}


class IgnoreEnums {

  enum WEEK_DAYS { MON, TUE, WED, THU, FRI, SAT, SUN }

  void test(WEEK_DAYS days) {
    switch (days) { // Compliant - enums are ignored
      case MON:
        break;
      case TUE:
        break;
      case WED:
        break;
      case THU:
        break;
      case FRI:
        break;
      case SAT:
        break;
      case SUN:
        break;
    }
  }

}
