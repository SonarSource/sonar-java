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
  //compile because it is not an override
  public boolean equals(int i) { // Compliant
  }
}

class H {
  public boolean equals(Object o) { // Noncompliant
  }

  public boolean equals(A o) {
  }
}

class I {
  class B {
    public boolean equals(Object o) { // Noncompliant
    }
  }
}

interface J {
  boolean equals(Object o); // Compliant
}

@interface K {
  int hashCode(); // Compliant
}

class L {
  public boolean equals(java.lang.Object o) { // Noncompliant
  }
}