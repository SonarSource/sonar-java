package checks;

class SynchronizedOverrideCheck {
  public synchronized void f1(){}
  public void f2(){}
  public synchronized void f3(){}
}

class SynchronizedOverrideCheck_B extends SynchronizedOverrideCheck {
  @Override
  public void f1(){} // Noncompliant [[sc=15;ec=17]] {{Make this method "synchronized" to match the parent class implementation.}}

  @Override
  public void f2(){} // Compliant

  public void f3(){} // Noncompliant [[secondary=6]] {{Make this method "synchronized" to match the parent class implementation.}}
}

class SynchronizedOverrideCheck_C extends SynchronizedOverrideCheck_B {
  @Override
  public void f1(){} // Compliant
}

class SynchronizedOverrideCheck_D extends java.lang.Throwable {
  @Override
  public Throwable getCause() { return null; } // Noncompliant {{Make this method "synchronized" to match the parent class implementation.}}
}

