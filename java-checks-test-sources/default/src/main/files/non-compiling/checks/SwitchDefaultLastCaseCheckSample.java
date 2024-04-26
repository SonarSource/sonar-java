package checks;

class SwitchDefaultLastCaseCheckSample {

  void foo(WeekendDay weekendDay) {
    int i = switch (0) {
      case 0:
      default: // Noncompliant {{Move this default to the end of the switch.}}
//    ^^^^^^^^
        yield 1;
      case 1:
        yield 2;
    };

    int j = switch (weekendDay) {
      case SATURDAY: // Compliant
        yield 1;
      case SUNDAY: // Compliant
        yield 1;
    };

    int k = switch (0) {
      default: // Compliant
        yield 1;
    };

    int l = switch (0) {
      case 0:
        yield 1;
      default: // Noncompliant
        yield 3;
      case 1:
        yield 2;
    };

    int l2 = switch (0) {
      case 0 -> 1;
      default -> 3; // Noncompliant
      case 1 -> 2;
    };

    int m = switch (0) {
      default: // Compliant
      case 0:
        yield 1;
      case 1:
        yield 2;
    };

    int n = switch (0) {
      default: // Noncompliant
        yield 1;
      case 0:
        yield 2;
      case 1:
        yield 3;
    };
  }

  public enum WeekendDay {
    SATURDAY, SUNDAY
  }

}
