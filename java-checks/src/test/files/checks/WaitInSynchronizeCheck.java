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
    obj.wait();    // Noncompliant [[sc=9;ec=13]] {{Move this call to "wait()" into a synchronized block to be sure the monitor on "String" is held.}}
    wait(1, 2); // Noncompliant {{Move this call to "wait()" into a synchronized block to be sure the monitor on "this" is held.}}
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

  synchronized Consumer<String> foo3() {
    wait(); // Compliant
    return s -> {
      wait(); // Noncompliant
      synchronized (obj) {
        wait(); // Compliant
      }
    };
  }
}
