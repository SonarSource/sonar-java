package checks;

class NPEThrowCheckSample {
  void foo() throws NullPointerException { // Noncompliant {{Use Objects.requireNonNull() instead of explicitly throwing NullPointerException.}}
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
  void qix() throws IllegalArgumentException {
    throw new IllegalArgumentException();
  }
}
