class A {
}

class A { // Noncompliant
  public boolean equals(Object o) {
  }
}

class A { // Noncompliant
  public int hashCode() {
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

enum A { // Noncompliant
  ;

  public boolean equals(Object o) {
  }
}
