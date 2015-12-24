class A {
  public String toString() {
    return "";
  }
  public String toString(int x) {
    return null;
  }
  public String notToString() {
    return null;
  }
}

class B {
  public String toString() {
    return null; // Noncompliant {{Return empty string instead.}}
  }  
}

class C {
  public String toString() {
    return (null); // Noncompliant [[sc=13;ec=17]]
  }
}
