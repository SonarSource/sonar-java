class A {
  private void equals() { // Noncompliant {{Either override Object.equals(Object), or totally rename the method to prevent any confusion.}}
  }

  private void equals(Object o) { // Compliant - methods cannot differ only by return type
  }

  private void equals(java.lang.Object o) { // Compliant
  }

  private boolean equals(Object o) { // Compliant
  }

  private boolean equals() { // Noncompliant
  }

  private boolean equals(Object o1, Object o2) { // Noncompliant
  }

  private boolean equals(Object foobar) { // Compliant
  }

  private boolean equals(int a) { // Noncompliant
  }

  private boolean equals(java.lang.Boolean a) { // Noncompliant
  }

  private boolean foo() { // Compliant
  }

  private boolean foo(Object o) {
  }

  private boolean EqUaLs() { // Noncompliant
  }
}
