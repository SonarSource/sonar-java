package checks;

class IdenticalCasesInSwitchCheckSample {
  void foo(WeekendDay weekendDay){
    int i = switch (weekendDay) {
      case SATURDAY:
        trivial();
        yield 1;
      case SUNDAY: // Noncompliant [[sc=7;el=+2;ec=17;secondary=-3]] {{This case's code block is the same as the block for the case on line 6.}}
        trivial();
        yield 1;
    };

    int j = switch (weekendDay) {
      case SATURDAY -> {
        trivial();
        yield 1;
      }
      case SUNDAY -> { // Noncompliant
        trivial();
        yield 1;
      }
    };

    int k = switch (weekendDay) {
      case SATURDAY:
        f(1);
        yield 1;
      case SUNDAY:
        f(2);
        yield 1;
    };

    int l = switch (1) {
      case 1:
        trivial();
        yield 1;
      case 2: // Compliant - this case is covered by RSPEC-3923
        trivial();
        yield 1;
      default: // Compliant - this case is covered by RSPEC-3923
        trivial();
        yield 1;
    };
  }

  private void trivial() {
  }

  private void f(int i) {
  }

  public enum WeekendDay {
    SATURDAY, SUNDAY
  }

}
