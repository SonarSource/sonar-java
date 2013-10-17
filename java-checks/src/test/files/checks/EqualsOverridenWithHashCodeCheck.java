class A {
}

class A {
  public boolean equals(Object o) { // Noncompliant
  }
}

class A {
  public int hashCode() { // Noncompliant
  }
}

class A {
  public boolean equals(Object o) {
  }
  public int hashCode() {
  }
}

class A {
  public boolean equals() {
  }
}

class A {
  public int hashCode(Object o) {
  }
}

enum A {
  ;

  public boolean equals(Object o) { // Noncompliant
  }
}

class A {
  public boolean equals(Object o) {
  }

  public boolean equals(A o) { // Noncompliant
  }
}

class A {
  class B {
    public boolean equals(Object o) { // Noncompliant
    }
  }
}

interface A {
  boolean equals(Object o); // Noncompliant
}

@interface A {
  int hashCode(); // Compliant
}
