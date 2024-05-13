package checks;

class AllBranchesAreIdentical {

  void switchStatement(WeekendDay weekendDay) {
    int i = switch (1) { // Noncompliant {{Remove this conditional structure or edit its code blocks so that they're not all the same.}}
//          ^^^^^^
      case 1:
        doSomething();
        yield 1;
      case 2:
        doSomething();
        yield 1;
      case 3:
        doSomething();
        yield 1;
      default:
        doSomething();
        yield 1;
    };

    int j = switch (1) { // Noncompliant
      case 1 -> 1;
      case 2 -> 1;
      case 3 -> 1;
      default -> 1;
    };


    int k = switch (weekendDay) { // Compliant as there is no "default" clause in this "switch" statement, this precise case is handled by RSPEC-1871
      case SATURDAY -> 1;
      case SUNDAY -> 1;
    };
  }

  private void doSomething() {
  }

  public enum WeekendDay {
    SATURDAY, SUNDAY
  }

}
