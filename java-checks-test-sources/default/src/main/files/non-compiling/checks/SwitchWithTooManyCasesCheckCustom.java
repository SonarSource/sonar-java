package checks;

class SwitchWithTooManyCasesCheckCustom {
  void foo() {
    int i = switch (1) { // Noncompliant {{Reduce the number of non-empty switch cases from 7 to at most 5.}}
      case 1:
        yield 1;
      case 2:
        yield 2;
      case 3:
        System.out.println("");
      case 4:
        yield 3;
      case 5:
        yield 4;
      case 6:
        yield 5;
      default:
        yield 6;
    };

    int j = switch (1) { // Noncompliant {{Reduce the number of non-empty switch cases from 7 to at most 5.}}
      case 1 -> 2;
      case 2 -> 3;
      case 3 -> 4;
      case 4 -> 5;
      case 5 -> 6;
      case 6 -> 7;
      default -> 8;
    };
  }

}
