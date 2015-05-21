import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyClass {



  Lock l1 = new ReentrantLock();
  Lock l2 = new ReentrantLock();
  Object a = null;

  public void acquireLock() {
    Lock lock = new ReentrantLock();
    lock.lock();  // Noncompliant
    l1.lock();  // Noncompliant
  }

  public void acquireAndReleaseLock() {
    Lock lock = new ReentrantLock();
    lock.lock();  // Compliant
    lock.unlock();
    l2.lock();
    l2.unlock();
  }

  public void releaseLock() {
    Lock lock = new ReentrantLock();
    lock.unlock();
  }

  public void unrelatedMethod() {
    Lock lock = new ReentrantLock();
    lock.lock(); // Noncompliant
    lock.toString();
  }

  public void doTheThing() {
    acquireLock();
    // do work...
    releaseLock();
  }

  public void multipleLockState() {
    Lock lock = new ReentrantLock();
    if(foo) {
      lock.lock();// Noncompliant
    } else {
      lock.lock();// Noncompliant
    }
  }

  public void doTheOtherThing() {
    Lock lock = new ReentrantLock();
    try {
      lock.tryLock();  // False negative Noncompliant
      // do work...
      lock.unlock(); // an exception will keep this line from being reached
    } catch (ExceptionType e) {
      // ...
    }
  }

  public void reassigned_lock() {
    Lock lock = new ReentrantLock();
    lock.tryLock(); // Noncompliant
    lock = new ReentrantLock();
    lock.lock();
    lock.unlock();
  }

  public void if_statement() {
    Lock lock = new ReentrantLock();
    boolean a;
    lock.tryLock();
    if(a) {
      lock.unlock();
    } else {
      lock.unlock();
    }
    lock = new ReentrantLock();
    lock.tryLock(); // Noncompliant
    if(a) {
      //lock not unlocked.
    } else {
      lock.unlock();
    }

    Lock l1 = new ReentrantLock();
    l1.lock(); // Noncompliant
    if(a){
      l1 = new ReentrantLock();
    } else {
      l1.unlock();
    }
  }

  public void switch_statement() {
    Lock lock = new ReentrantLock();
    lock.tryLock(); // Noncompliant
    switch (foo) {
      case 0:
        System.out.println("");
      case 1:
        lock.unlock();
        break;
    }
    lock = new ReentrantLock();
    lock.tryLock();
    switch (foo) {
      case 0:
        System.out.println("");
      case 1:
        lock.unlock();
        break;
      default:
        lock.unlock();
    }
    lock = new ReentrantLock();
    lock.tryLock(); // False negative Noncompliant
    switch (foo) {
      case 0:
        System.out.println("");
        break;
      case 1:
        lock.unlock();
        break;
      default:
        lock.unlock();
    }
  }

  public void tryStatement() {
    Lock lock = new ReentrantLock();
    try {
      lock.tryLock();
    } catch (MyException e) {
    } catch (MyOtherException e) {
    } finally {
      lock.unlock();
    }
    lock = new ReentrantLock();
    try {
      lock.tryLock(); // Noncompliant
    } catch (MyException e) {
      lock.unlock();
    } catch (MyOtherException e) {
      lock.unlock();
    }
    lock = new ReentrantLock();
    try {
      lock.tryLock(); // Noncompliant
    } catch (MyException e) {
    } catch (MyOtherException e) {
      lock.unlock();
    }
    ReentrantLock lock2 = new ReentrantLock();
    lock2.lock();
    try {
      System.out.println("");
      return;
    } finally {
      lock2.unlock();
    }
  }

  public void while_statement() {
    Lock lock = new ReentrantLock();
    while (foo) {
      lock.tryLock();
    }
    lock.unlock();
    while (foo) {
      lock.tryLock(); // Noncompliant
      lock =  new ReentrantLock();
    }
    lock.unlock();
  }

  public void doubleLock() {
    Lock lock = new ReentrantLock();
    lock.tryLock();
    lock.tryLock();
    lock.unlock();
  }

  public void do_while_statement() {
    Lock lock = new ReentrantLock();
    do {
      lock.tryLock();
    } while (foo);
    lock.unlock();
    do {
      lock.tryLock(); // Noncompliant
      lock =  new ReentrantLock();
    } while (foo);
    lock.unlock();
  }

  public void for_statement() {
    Lock lock = new ReentrantLock();
    for (int i = 0; i <10; i++) {
      lock.tryLock(); // Noncompliant
      lock =  new ReentrantLock();
    }
    lock.unlock();

    for (Foo foo : foos) {
      lock.tryLock(); // Noncompliant
      lock =  new ReentrantLock();
    }
    lock.unlock();
  }

  void if_try_lock() {
    Lock lock = new ReentrantLock();
    //False positive
    if(lock.tryLock()) { // Noncompliant
      try {

      } finally {
        lock.unlock();
      }
    }
  }
}
