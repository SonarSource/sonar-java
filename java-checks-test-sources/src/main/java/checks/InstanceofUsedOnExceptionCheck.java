package checks;

import java.io.IOException;

class InstanceofUsedOnExceptionCheck {
  private void withEalseIf() {
    try {
    } catch (Exception e) {
      if (e instanceof IOException) { // Noncompliant

      } else if (e instanceof IllegalArgumentException) { // Noncompliant

      }
    }

    try {
    } catch (Exception e) {
      if (e instanceof IOException) { // Noncompliant

      } else if (e instanceof IllegalArgumentException && anotherCondition()) { // Compliant, not possible to refactor in a nicer way

      }
    }
  }

  private void f(MyException foo) {
    try {
    } catch (Exception e) {
      if (e instanceof IOException) {} // Noncompliant [[sc=13;ec=23]] {{Replace the usage of the "instanceof" operator by a catch block.}}
    }
    try {
    } catch (Exception e) {
      if (e instanceof MyInterface) {}
    }
    try {
    } catch (Exception e) {
      if (foo instanceof IOException) {}
    }
    try {
    } catch (Exception e) {
      if (foo instanceof e) {}
    }
    try {
    } catch (Exception e) {
      if (foo.foo instanceof IOException) {}
    }
    try {
    } catch (Exception e) {
      if (e
        instanceof IOException) {} // Noncompliant
    }
    Object e = 0;
    if (e instanceof Integer) {}
  }

  private void withMoreCode() {
    try {
    } catch (Exception e) {
      if (e instanceof IOException) { } // Compliant, refactoring results in duplication
      doSomething();
    }

    try {
    } catch (Exception e) {
      doSomething();
      if (e instanceof IOException) { } // Compliant
    }

    try {
    } catch (Exception e) {
      doSomething();
      if (e instanceof IOException) { } // Compliant
      doSomethingElse();
    }
  }

  private void withoutTrivialInstanceOf() {
    try {
    } catch (Exception e) {
      if (e instanceof IOException) { // Noncompliant
        doSomething();
      }
      if (e instanceof IllegalArgumentException && anotherCondition()) { // Compliant, not possible to refactor in a nicer way
        doSomethingElse();
      }
    }

    try {
    } catch (Exception e) {
      if (e instanceof IOException) { // Noncompliant
        doSomething();
      } else {
        doSomething();
      }
    }
  }

  private void withElseIf() {
    try {
    } catch (Exception e) {
      if (e instanceof IOException) { // Noncompliant
        doSomething();
      } else if (e instanceof IllegalArgumentException) { // Noncompliant
        doSomethingElse();
      }
    }

    try {
    } catch (Exception e) {
      if (e instanceof IOException) { // Noncompliant

        doSomething();
      } else if (e instanceof IllegalArgumentException && anotherCondition()) { // Compliant, not possible to refactor in a nicer way

      } else if (e instanceof MyException) { // Noncompliant

        doSomethingElse();
        doSomething();
      }
    }
  }

  private void withThrow() throws IllegalAccessException {
    try {
    } catch (Exception e) {
      if (e instanceof IOException) {// Noncompliant
        throw new IllegalAccessException("");
      }
      throw e;
    }

    try {
    } catch (Exception e) {
      if (e instanceof IOException) { // Noncompliant
        doSomething();
      }
      throw e;
    }

    try {
    } catch (Exception e) {
      if (e instanceof IOException && anotherCondition()) { // Compliant, not possible to refactor in a nicer way
        throw new IllegalAccessException("");
      }
      throw e;
    }
  }

  private int withReturn() {
    try {
    } catch (Exception e) {
      if (e instanceof IOException) {// Noncompliant
        return 1;
      }
      return 2;
    }

    try {
    } catch (Exception e) {
      if (e instanceof IOException && anotherCondition()) { // Compliant, not possible to refactor in a nicer way
        return 1;
      }
      return 2;
    }
    return 0;
  }

  private boolean anotherCondition() {
    return true;
  }

  private void doSomething() {
  }

  private void doSomethingElse() {
  }

  private static class MyException extends IOException implements MyInterface {
    Exception foo;
  }

  interface e {}
  interface MyInterface {}
}
