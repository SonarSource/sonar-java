package checks;

class NPEThrowCheckSample {
  void foo() throws NullPointerException { // Noncompliant {{Throw some other exception here, such as "IllegalArgumentException".}}
//                  ^^^^^^^^^^^^^^^^^^^^
  }
  void bar() {
    throw new
    NullPointerException // Noncompliant {{Throw some other exception here, such as "IllegalArgumentException".}}
    ();
  }
  void baz() {
    throw new java.lang.NullPointerException(); // Noncompliant {{Throw some other exception here, such as "IllegalArgumentException".}}
//            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }
  void qix() throws IllegalArgumentException {
    throw new IllegalArgumentException();
  }
}
