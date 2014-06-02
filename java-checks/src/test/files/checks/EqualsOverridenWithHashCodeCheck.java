class A {
}

class B {
  public boolean equals(Object o) { // Noncompliant
  }
}

class C {
  public int hashCode() { // Noncompliant
  }
}

class D {
  public boolean equals(Object o) {
  }
  public int hashCode() {
  }
}

class E {
  public boolean equals() {
  }
}

class F {
  public int hashCode(Object o) {
  }
}

enum G {
  ;

  public boolean equals(Object o) { // Noncompliant
  }
}

class H {
  public boolean equals(Object o) {
  }

  public boolean equals(A o) { // Noncompliant
  }
}

class I {
  class B {
    public boolean equals(Object o) { // Noncompliant
    }
  }
}

interface J {
  boolean equals(Object o); // Noncompliant
}

@interface K {
  int hashCode(); // Compliant
}
