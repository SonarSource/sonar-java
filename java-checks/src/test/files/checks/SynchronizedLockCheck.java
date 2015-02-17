import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

class A {
  void foo() {
    Lock lock = new MyLockImpl();
    synchronized (lock) { // Noncompliant
    }
    synchronized (new MyLockImpl()) { // Noncompliant
    }
    synchronized (new A()) { // Compliant
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