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
    synchronized (this) { // Compliant
    }
    synchronized (bar()) { // Compliant
    }
  }
  
  B bar() {
    return new B();
  }
}

class B {
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