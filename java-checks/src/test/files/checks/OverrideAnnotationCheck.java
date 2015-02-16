class A {
  void foo(){} // Compliant
}
interface I {
  public void finalize(); // Noncompliant
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
  void foo() {} //NonCompliant
  void bar() {} //Noncompliant
  @Override
  protected void finalize() { // Compliant
    super.finalize();
  }
}

