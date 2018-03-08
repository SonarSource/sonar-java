class A{
  public boolean equals(@javax.annotation.Nonnull Object obj) { // Noncompliant [[sc=25;ec=61]] {{"equals" method parameters should not be marked "@Nonnull".}}
    return true;
  }
}

class B {
  public boolean equals(Object obj) { // Compliant
  }
}

class C {
  public boolean equal() {}  // Compliant

  public boolean equals(Object a, Object b) {  // Compliant
  }
}

class D {
  public boolean equals(C c) {  // Compliant
    return false;
  }
}

class E {
  public boolean equals(@javax.annotation.Nonnull C c) {  // Compliant
    return false;
  }
}
