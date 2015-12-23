class A {
  void foo(){} // Compliant
}
interface I {
  public void finalize(); // Noncompliant [[sc=15;ec=23]] {{Add the "@Override" annotation above this method signature}}
  void bar(); // Compliant
}
interface J extends I {
  public void finalize(); // Noncompliant
  void bar(); // Noncompliant
}
interface K extends J {
  public boolean equals(Object obj); // Noncompliant
}
class B extends A implements I {
  void foo() {} // Noncompliant
  void bar() {} // Noncompliant
  @Override
  protected void finalize() { // Compliant
    super.finalize();
  }
}

