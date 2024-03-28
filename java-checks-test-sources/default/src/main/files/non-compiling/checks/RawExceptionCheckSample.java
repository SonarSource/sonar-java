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

  void doSomething() { return null; }
  void doSomethingElse() throws Exception { return null; } // Noncompliant
}
