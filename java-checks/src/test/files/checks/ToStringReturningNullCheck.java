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

class D {
  protected Object clone() {
    return null; // Noncompliant [[sc=12;ec=16]] {{Return a non null object.}}
  }
}
