final class A {
  
  int w;
  private int x;
  protected int y; // Noncompliant
  public int z;

  static {
  }
  
  void method1() {}
  private void method2() {}
  protected void // Noncompliant [[sc=3;ec=12]] {{Remove this "protected" modifier.}}
      method3() {}
  public void method4() {}
}

class B {
  protected int y;
  protected void method3() {}
}
final class C extends B {
  protected void method3() {} //Compliant method is overriden
}

final class D {
  @com.google.common.annotations.VisibleForTesting
  protected int xx; //Compliant @VisibleForTesting is used
  @com.google.common.annotations.VisibleForTesting
  protected void method5() {} //Compliant @VisibleForTesting is used
}
