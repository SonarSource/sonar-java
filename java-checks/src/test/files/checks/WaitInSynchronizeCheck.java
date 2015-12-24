class A {
  java.lang.String obj;
  void qix() {
    synchronized (obj) {
      wait();
      wait(1, 2);
      wait(1);
      notify();
      notifyAll();
    }
  }
  synchronized void foo() {
    wait();
    wait(1, 2);
    wait(1);
    notify();
    notifyAll();
  }
  void bar() {
    obj.wait();    // Noncompliant [[sc=9;ec=13]] {{Make this call to "wait()" only inside a synchronized block to be sure to hold the monitor on "String" object.}}
    wait(1, 2); // Noncompliant {{Make this call to "wait()" only inside a synchronized block to be sure to hold the monitor on "this" object.}}
    wait(1);   // Noncompliant
    notify();  // Noncompliant
    notifyAll(); // Noncompliant
  }

  synchronized void foo2() {
    wait();
    class A {
      void foo() {
        wait(); // Noncompliant
      }
    }
    wait();
  }


}
