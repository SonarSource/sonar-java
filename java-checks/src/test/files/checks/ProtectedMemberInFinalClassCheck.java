final class A {
  
  int w;
  private int x;
  protected int y; // Noncompliant
  public int z;
  
  void method1() {}
  private void method2() {}
  protected void
      method3() {} // Noncompliant
  public void method4() {}
}

class B {
  protected int y;
  protected void method3() {}
}
final class C extends B {
  protected void method3() {} //Compliant method is overriden
}
