package checks;

class RawExceptionCheckSample {
  void foo() throws Exception { // Noncompliant
    doSomething();
  }

  void bar() throws Exception { // Compliant
    doSomethingElse();
  }

  void qix() throws Exception {
    unknown();
  }

  void wraps_unresolved_checked_exception() {
    try {
      unknown();
    } catch (SpecificCheckedException e) {
      throw new RuntimeException(e); // Compliant
    }
  }

  void doSomething() { return null; }
  void doSomethingElse() throws Exception { return null; } // Noncompliant
}
