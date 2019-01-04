class A {
  public synchronized void f1(){}
  public void f2(){}
  public synchronized void f3(){}
}

class B extends A{
  @java.lang.Override
  public void f1(){} // Noncompliant [[sc=15;ec=17]] {{Make this method "synchronized" to match the parent class implementation.}}

  @java.lang.Override
  public void f2(){} // Compliant

  public void f3(){} // Noncompliant [[secondary=4]] {{Make this method "synchronized" to match the parent class implementation.}}
}

class C extends B{
  @java.lang.Override
  public void f1(){} // Compliant
}

class D extends java.lang.Throwable {
  @java.lang.Override
  public Throwable getCause() {} // Noncompliant {{Make this method "synchronized" to match the parent class implementation.}}
}

