package symbolicexecution.checks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract class LocksNotUnlockedCheck {

  public void while_statement() {
    Lock lock = new ReentrantLock();
    while (foo) {
      lock.tryLock();
    }
    lock.unlock();
    while (foo) {
      lock.tryLock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
      lock =  new ReentrantLock();
    }
    lock.unlock();
  }

}
