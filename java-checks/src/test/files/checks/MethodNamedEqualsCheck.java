class A {
  private void equals() { // Non-Compliant
  }

  private void equals(Object o) { // Compliant - methods cannot differ only by return type
  }

  private void equals(java.lang.Object o) { // Compliant
  }

  private boolean equals(Object o) { // Compliant
  }

  private boolean equals() { // Non-Compliant
  }

  private boolean equals(Object o1, Object o2) { // Non-Compliant
  }

  private boolean equals(Object foobar) { // Compliant
  }

  private boolean equals(int a) { // Non-Compliant
  }

  private boolean equals(java.lang.Boolean a) { // Non-Compliant
  }

  private boolean foo() { // Compliant
  }

  private boolean foo(Object o) {
  }

  private boolean EqUaLs() { // Non-Compliant
  }
}
