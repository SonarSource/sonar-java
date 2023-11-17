package symbolicexecution.checks;

public abstract class ConditionAlwaysTrueOrFalseNoExceptionOnBranchingStatementsLogicalAndOrTestCase {
  boolean aBoolean;
  final void foo() {}

  final void if_and() {
    while (true) {
      if (false && aBoolean) // Noncompliant
        foo();

      foo();
    }
  }

  final void if_or() {
    while (true) {
      if (true || aBoolean) // Noncompliant
        foo();

      foo();
    }
  }

  final void while_and() {
    while (true) {
      while (false && aBoolean) // Noncompliant
        foo();

      foo();
    }
  }

  final void for_and() {
    while (true) {
      for (; false && aBoolean ; ) // Noncompliant
        foo();

      foo();
    }
  }

  final void do_and() {
    while (true) {
      do {
        foo();
      } while (false && false); // Noncompliant

      foo();
    }
  }
}
