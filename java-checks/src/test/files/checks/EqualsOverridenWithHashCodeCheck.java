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
