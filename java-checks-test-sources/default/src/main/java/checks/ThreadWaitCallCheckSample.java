package checks;

import java.lang.Override;
import java.lang.Thread;

class ThreadWaitCallCheckSample {

  void foo() throws InterruptedException {
    new A().wait();
    new A().wait(1l);
    new A().wait(1,1);
    new A().notify();
    new A().notifyAll();

    new B().wait(); // Noncompliant {{Refactor the synchronisation mechanism to not use a Thread instance as a monitor}}
    new B().wait(1000); // Noncompliant [[sc=13;ec=17]]
    new B().wait(12,12); // Noncompliant
    new B().notify(); // Noncompliant
    new B().notifyAll(); // Noncompliant
  }

  class A {

  }

  class B extends Thread {
    @Override
    public void run() {

    }
  }


}
