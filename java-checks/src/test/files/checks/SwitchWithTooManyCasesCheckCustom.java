class A{
  void foo() {
    switch (1) { // Noncompliant {{Reduce the number of non-empty switch cases from 35 to at most 5.}}
      case 1:
        System.out.println("");
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
        System.out.println("");
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
      case 1:
        break;
        System.out.println("");
      default:
        System.out.println("");
    }
    switch (1) { // Noncompliant [[secondary=78,80,82,84,86,88]]
      case 1:
        break;
      case 1:
        break;
      case 1:
        System.out.println("");
      case 1:
        break;
      case 1:
        break;
      case 1:
      case 1:
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
