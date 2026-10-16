package checks;

class NPEThrowCheckSample {
  void foo() throws NullPointerException { // Noncompliant {{Remove NullPointerException from the throws clause.}}
//                  ^^^^^^^^^^^^^^^^^^^^
  }
  void bar() {
    throw new
    NullPointerException // Noncompliant {{Use Objects.requireNonNull() instead of explicitly throwing NullPointerException.}}
    ();
  }
  void baz() {
    throw new java.lang.NullPointerException(); // Noncompliant {{Use Objects.requireNonNull() instead of explicitly throwing NullPointerException.}}
//            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }
  void withMessage(Object o) {
    throw new NullPointerException("o must not be null"); // Compliant, a message is provided
  }
  void withComputedMessage(String name) {
    throw new java.lang.NullPointerException(name + " must not be null"); // Compliant
  }
  void qix() throws IllegalArgumentException {
    throw new IllegalArgumentException();
  }
}
