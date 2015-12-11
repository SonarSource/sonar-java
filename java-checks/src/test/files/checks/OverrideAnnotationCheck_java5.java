class A {
  void foo(){} // Compliant
}
interface I {
  public void finalize();
  void bar(); // Compliant
}
interface J extends I {
  public void finalize();
  void bar();
}
interface K extends J {
  public boolean equals(Object obj);
}
class B extends A implements I {
  void foo() {} // Noncompliant
  void bar() {}
  @Override
  protected void finalize() { // Compliant
    super.finalize();
  }
}

