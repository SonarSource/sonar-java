import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyClass {
    Lock lock = new ReentrantLock();

  public void acquireLock() {
    lock.lock();  // Noncompliant
  }

  public void acquireAndReleaseLock() {
    lock.lock();  // Compliant
    lock.unlock();
  }

  public void releaseLock() {
    lock.unlock();
  }

  public void doTheThing() {
    acquireLock();
    // do work...
    releaseLock();
  }

  public void doTheOtherThing() {
    try {
      lock.tryLock();  // False negative Noncompliant
      // do work...
      lock.unlock(); // an exception will keep this line from being reached
    } catch (ExceptionType e) {
      // ...
    }
  }

  public void reassigned_lock() {
    lock.tryLock(); // Noncompliant
    lock = new ReentrantLock();
    lock.lock();
    lock.unlock();
  }

  public void if_statement() {
    boolean a;
    lock.tryLock();
    if(a) {
      lock.unlock();
    } else {
      lock.unlock();
    }
    lock.tryLock(); // Noncompliant
    if(a) {
      //lock not unlocked.
    } else {
      lock.unlock();
    }
  }

  public void switch_statement() {
    lock = new ReentrantLock();
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
    try {
      lock.tryLock();
    } catch (MyException e) {
    } catch (MyOtherException e) {
    } finally {
      lock.unlock();
    }
    lock = new ReentrantLock(); // Noncompliant
    try {
      lock.tryLock();
    } catch (MyException e) {
      lock.unlock();
    } catch (MyOtherException e) {
      lock.unlock();
    }
    lock = new ReentrantLock(); // Noncompliant
    try {
      lock.tryLock();
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
    while (foo) {
      //False positive
      lock.tryLock();  // Noncompliant
    }
    lock.unlock();
    while (foo) {
      lock.tryLock(); // Noncompliant
      lock =  new ReentrantLock();
    }
    lock.unlock();
  }

  public void doubleLock() {
    lock.tryLock(); // Noncompliant
    lock.tryLock();
    lock.unlock();
  }

  public void do_while_statement() {
    do {
      //False positive
      lock.tryLock(); // Noncompliant
    } while (foo);
    lock.unlock();
    do {
      lock.tryLock(); // Noncompliant
      lock =  new ReentrantLock();
    } while (foo);
    lock.unlock();
  }

  public void for_statement() {
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



}
