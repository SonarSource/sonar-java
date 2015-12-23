class A {
  Object monitor;
  void foo() {
    Thread.sleep(12);
    Thread.sleep(12, 13);
    synchronized(monitor) {
      while(notReady()){
        Thread.sleep(200);    // Noncompliant [[sc=16;ec=21]] {{Replace the call to "Thread.sleep(...)" with a call to "wait(...)".}}
        Thread.sleep(200, 12); // Noncompliant
      }
      process();
    }
  }

  synchronized void foo() {
    Thread.sleep(200);    // Noncompliant
    Thread.sleep(200, 12); // Noncompliant
  }
}
