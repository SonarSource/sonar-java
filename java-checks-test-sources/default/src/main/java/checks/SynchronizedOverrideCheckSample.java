package checks;

class SynchronizedOverrideCheckSample {
  public synchronized void f1(){}
  public void f2(){}
  public synchronized void f3(){}
//  ^^^<
}

class SynchronizedOverrideCheckSample_B extends SynchronizedOverrideCheckSample {
  @Override
  public void f1(){} // Noncompliant {{Make this method "synchronized" to match the parent class implementation.}}
//            ^^

  @Override
  public void f2(){} // Compliant

  public void f3(){} // Noncompliant {{Make this method "synchronized" to match the parent class implementation.}}
}

class SynchronizedOverrideCheckSample_C extends SynchronizedOverrideCheckSample_B {
  @Override
  public void f1(){} // Compliant
}

class SynchronizedOverrideCheckSample_D extends java.lang.Throwable {
  @Override
  public Throwable getCause() { return null; } // Noncompliant {{Make this method "synchronized" to match the parent class implementation.}}
}

