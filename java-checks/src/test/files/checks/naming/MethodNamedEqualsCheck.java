class C {
  public boolean equals(Boolean b) {  // Noncompliant [[sc=18;ec=24]] {{Either override Object.equals(Object), or rename the method to prevent any confusion.}}
    return b;
  }
}

class OverridesEquals {
  public boolean equals(Object o) { // Compliant
  }

  private void equals() {
  }

  private void equals(Object o) { // Compliant - methods cannot differ only by return type
  }

  private void equals(java.lang.Object o) { // Compliant
  }

  private boolean equals() {
  }

  private boolean equals(Object o1, Object o2) {
  }

  private boolean equals(Object foobar) { // Compliant
  }

  private boolean equals(int a) {
  }

  private boolean equals(java.lang.Boolean a) {
  }

  private boolean foo() { // Compliant
  }

  private boolean foo(Object o) {
  }

  private boolean EqUaLs() {
  }
}

interface I {
  boolean equals(Integer i, Integer y);
}

class B implements I {
  @Override
  public boolean equals(Integer i, Integer y) { // Compliant
    return false;
  }
}

