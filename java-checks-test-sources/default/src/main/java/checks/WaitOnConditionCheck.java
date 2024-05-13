package checks;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

class WaitOnConditionCheck {
  void foo() throws InterruptedException {
    new C().wait();
    new C().wait(1);
    new C().wait(1, 3);
    new B().wait(); // Noncompliant {{The "Condition.await(...)" method should be used instead of "Object.wait(...)"}}
//          ^^^^
    MyFunctionalInterface r = new B()::wait; // Noncompliant {{The "Condition.await(...)" method should be used instead of "Object.wait(...)"}}
//                                     ^^^^
    new B().wait(1); // Noncompliant
    new B().wait(1, 3); // Noncompliant
  }

  class B implements Condition {
    @Override public void await() throws InterruptedException { }
    @Override public void awaitUninterruptibly() { }
    @Override public long awaitNanos(long nanosTimeout) throws InterruptedException { return 0; }
    @Override public boolean await(long time, TimeUnit unit) throws InterruptedException { return false; }
    @Override public boolean awaitUntil(Date deadline) throws InterruptedException { return false; }
    @Override public void signal() { }
    @Override public void signalAll() { }
  }
  class C { }

  interface MyFunctionalInterface {
    void run() throws InterruptedException;
  }
}
