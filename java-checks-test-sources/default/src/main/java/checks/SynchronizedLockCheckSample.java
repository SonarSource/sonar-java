package checks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

class SynchronizedLockCheckSample {
  void foo() {
    Lock lock = new MyLockImpl();
    synchronized (lock) { // Noncompliant {{Synchronize on this "Lock" object using "acquire/release".}}
//                ^^^^
    }
    synchronized (new MyLockImpl()) { // Noncompliant {{Synchronize on this "Lock" object using "acquire/release".}}
    }
    synchronized (new UselessIncrementCheck()) { // Compliant
    }
  }
}

class MyLockImpl implements Lock {
  @Override
  public void lock() {
  }
  @Override
  public void lockInterruptibly() throws InterruptedException {
  }
  @Override
  public boolean tryLock() {
    return false;
  }
  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    return false;
  }
  @Override
  public void unlock() {
  }
  @Override
  public Condition newCondition() {
    return null;
  }
}
