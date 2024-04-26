package symbolicexecution.checks;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract class LocksNotUnlockedCheckSample {

  Lock l1 = new ReentrantLock();
  Lock l2 = new ReentrantLock();
  Boolean a = null;
  boolean foo = false;
  Integer fooInt = 12;

  public void acquireLock() {
    Lock local = new ReentrantLock();
    local.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    l1.lock();
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
    lock.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    lock.toString();
  }

  public void lock_interruptibly() throws InterruptedException {
    Lock lock = new ReentrantLock();
    lock.lockInterruptibly();
    lock.unlock();
    lock.lockInterruptibly(); // Compliant
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
      lock.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    } else {
      lock.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    }
  }

  public void doTheOtherThing() {
    Lock lock = new ReentrantLock();
    try {
      lock.tryLock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
      doSomething();
      lock.unlock(); // an exception will keep this line from being reached
    } catch (Exception e) {
      // ...
    }
  }

  abstract void doSomething() throws Exception;

  public void reassigned_lock() {
    Lock lock = new ReentrantLock();
    lock.tryLock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    lock = new ReentrantLock();
    lock.lock();
    lock.unlock();
  }

  public void if_statement() {
    Lock lock = new ReentrantLock();
    boolean a = lock.tryLock(); // Compliant
    if(a) {
      lock.unlock();
    } else {
      System.out.println("Lock was not granted!");
    }
  }

  void if_try_lock() {
    Lock lock = new ReentrantLock();
    if(lock.tryLock()) { // Compliant
      try {

      } finally {
        lock.unlock();
      }
    }
  }

  public void if_statement_missing() {
    Lock lock = new ReentrantLock();
    lock.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    if(a) {
      //lock never unlocked.
    } else {
      lock.unlock();
    }
  }

  public void if_statement_overwrite() {
    Lock l1 = new ReentrantLock();
    l1.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    if(a){
      l1 = new ReentrantLock();
    } else {
      l1.unlock();
    }
  }

  public void switch_statement() {
    Lock lock = new ReentrantLock();
    lock.tryLock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    switch (fooInt) {
      case 0:
        System.out.println("");
      case 1:
        lock.unlock();
        break;
    }
    lock = new ReentrantLock();
    lock.tryLock();
    switch (fooInt) {
      case 0:
        System.out.println("");
      case 1:
        lock.unlock();
        break;
      default:
        lock.unlock();
    }
    lock = new ReentrantLock();
    lock.tryLock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    switch (fooInt) {
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
      lock.tryLock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    } catch (MyException e) {
      lock.unlock();
    } catch (MyOtherException e) {
      lock.unlock();
    }
    lock = new ReentrantLock();
    try {
      lock.tryLock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    } catch (MyException e) {
    } catch (MyOtherException e) {
      lock.unlock();
    }
    ReentrantLock lock2 = new ReentrantLock();
    lock2.lock();
    try {
      System.out.println("");
    } finally {
      lock2.unlock();
    }
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
      lock.tryLock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
      lock =  new ReentrantLock();
    } while (foo);
    lock.unlock();
  }

  public void for_statement() {
    Lock lock = new ReentrantLock();
    for (int i = 0; i <10; i++) {
      lock.tryLock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
      lock =  new ReentrantLock();
    }
    lock.unlock();

    for (String foo : new ArrayList<String>()) {
      lock.tryLock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
      lock =  new ReentrantLock();
    }
    lock.unlock();
  }

  public void unlockingInTryCatch() {
    Lock lock = new ReentrantLock();
    try {
      lock.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    } finally {
      try {
        lock.unlock();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  public void unlockingInTryCatchButMissed() {
    Lock lock = new ReentrantLock();
    try {
      lock.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
    } finally {
      try {
        lock.unlock();
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        cleanUp();
      }
    }
  }

  protected abstract void cleanUp();

  private volatile ScheduledExecutorService executorService;
  Lock lock = new ReentrantLock();

  protected final void doStart() {
    executorService.execute(new Runnable() {
      @Override public void run() {
        lock.lock();
        try {
          startUp();
        } catch (Throwable t) {
          cancel();
        } finally {
          lock.unlock();
        }
      }

      private void cancel() {
      }

      private void startUp() {
      }
    });
  }

  private final AbstractService delegate = new AbstractService() {
    private final ReentrantLock lock = new ReentrantLock();
    protected final void doStartWithinDelegate() {
      executorService.execute(new Runnable() {
        @Override public void run() {
          lock.lock();
          try {
            startUp();
          } finally {
            lock.unlock();
          }
        }
        private void startUp() {
        }
      });
    }
  };


  private static class MyLock extends ReentrantLock {

    @Override
    public void lock() {
      try {
        super.lock();
      } finally {
        logLocked();
      }
    }

    private void logLocked() {
    }
  }

  public class SonarFail {
    java.util.List<Lock> list;
    Object test(Lock local) {
      Lock l1 = list.get(0);
      local.lock();
      try {
        if (l1 == local) {
          return null;
        }
      } finally {
        local.unlock();
      }
      return new Object();
    }

    void t2(Lock local) {
      Lock l1 = list.get(0);
      try {
        if (l1 == local) {
          l1.lock();
        }
      } finally {
        local.unlock();
      }
    }
  }

  class MyException extends RuntimeException {
  }

  class MyOtherException extends RuntimeException {
  }
  abstract class AbstractService {
  }
}
