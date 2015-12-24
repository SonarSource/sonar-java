class A {
  
  private final Boolean bLock = Boolean.FALSE;
  private final Integer iLock = Integer.valueOf(0);
  private final String sLock = "LOCK";
  private final Object oLock = new Object();
  
  void method1() {
    
    synchronized(bLock) {  // Noncompliant [[sc=18;ec=23]] {{Synchronize on a new "Object" instead.}}
      // ...
    }
    synchronized(iLock) {  // Noncompliant
      // ...
    }
    synchronized(sLock) {  // Noncompliant
      // ...
    }
    synchronized(42) {  // Noncompliant
      // ...
    }
    synchronized(oLock) {
      // ...
    }
  }
  
}
