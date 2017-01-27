import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyClass {

  Lock l1 = new ReentrantLock();
  Lock l2 = new ReentrantLock();
  Object a = null;

  public void acquireLock() {
    Lock local = new ReentrantLock();
    local.lock();  // Noncompliant [[flows=acquire]] {{Unlock this lock along all executions paths of this method.}} flow@acquire {{Lock 'local' is never unlocked.}}
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
    lock.lock(); // Noncompliant [[flows=unrelated]] {{Unlock this lock along all executions paths of this method.}} flow@unrelated {{Lock 'lock' is never unlocked.}}
    lock.toString();
  }

  public void lock_interruptibly() {
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
      lock.lock();// Noncompliant [[flows=multiple1]] {{Unlock this lock along all executions paths of this method.}} flow@multiple1 {{Lock 'lock' is never unlocked.}}
    } else {
      lock.lock();// Noncompliant [[flows=multiple2]] {{Unlock this lock along all executions paths of this method.}} flow@multiple2 {{Lock 'lock' is never unlocked.}}
    }
  }

  public void doTheOtherThing() {
    Lock lock = new ReentrantLock();
    try {
      lock.tryLock();  // Noncompliant [[flows=doOther]] {{Unlock this lock along all executions paths of this method.}} flow@doOther {{Lock 'lock' is never unlocked.}}
      doSomething();
      lock.unlock(); // an exception will keep this line from being reached
    } catch (Exception e) {
      // ...
    }
  }

  abstract void doSomething() throws Exception;

  public void reassigned_lock() {
    Lock lock = new ReentrantLock();
    lock.tryLock(); // Noncompliant [[flows=reassigned]] {{Unlock this lock along all executions paths of this method.}} flow@reassigned {{Lock 'lock' is never unlocked.}}
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
    lock.lock(); // Noncompliant [[flows=ifMissing]] {{Unlock this lock along all executions paths of this method.}} flow@ifMissing {{Lock 'lock' is never unlocked.}}
    if(a) {
      //lock never unlocked.
    } else {
      lock.unlock();
    }
  }

  public void if_statement_overwrite() {
    Lock l1 = new ReentrantLock();
    l1.lock(); // Noncompliant [[flows=ifOverwrite]] {{Unlock this lock along all executions paths of this method.}} flow@ifOverwrite {{Lock 'l1' is never unlocked.}}
    if(a){
      l1 = new ReentrantLock();
    } else {
      l1.unlock();
    }
  }

  public void switch_statement() {
    Lock lock = new ReentrantLock();
    lock.tryLock(); // Noncompliant [[flows=switch]] {{Unlock this lock along all executions paths of this method.}} flow@switch {{Lock 'lock' is never unlocked.}}
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
    lock.tryLock(); // Noncompliant [[flows=switch2]] {{Unlock this lock along all executions paths of this method.}} flow@switch2 {{Lock 'lock' is never unlocked.}}
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
      lock.tryLock(); // Noncompliant [[flows=try]] {{Unlock this lock along all executions paths of this method.}} flow@try {{Lock 'lock' is never unlocked.}}
    } catch (MyException e) {
      lock.unlock();
    } catch (MyOtherException e) {
      lock.unlock();
    }
    lock = new ReentrantLock();
    try {
      lock.tryLock(); // Noncompliant [[flows=try2]] {{Unlock this lock along all executions paths of this method.}} flow@try2 {{Lock 'lock' is never unlocked.}}
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

  public void while_statement() {
    Lock lock = new ReentrantLock();
    while (foo) {
      lock.tryLock();
    }
    lock.unlock();
    while (foo) {
      lock.tryLock(); // Noncompliant [[flows=while]] {{Unlock this lock along all executions paths of this method.}} flow@while {{Lock 'lock' is never unlocked.}}
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
      lock.tryLock(); // Noncompliant [[flows=doWhile]] {{Unlock this lock along all executions paths of this method.}} flow@doWhile {{Lock 'lock' is never unlocked.}}
      lock =  new ReentrantLock();
    } while (foo);
    lock.unlock();
  }

  public void for_statement() {
    Lock lock = new ReentrantLock();
    for (int i = 0; i <10; i++) {
      lock.tryLock(); // Noncompliant [[flows=for]] {{Unlock this lock along all executions paths of this method.}} flow@for {{Lock 'lock' is never unlocked.}}
      lock =  new ReentrantLock();
    }
    lock.unlock();

    for (Foo foo : foos) {
      lock.tryLock(); // Noncompliant [[flows=foreach]] {{Unlock this lock along all executions paths of this method.}} flow@foreach {{Lock 'lock' is never unlocked.}}
      lock =  new ReentrantLock();
    }
    lock.unlock();
  }

  public void unlockingInTryCatch() {
    Lock lock = new ReentrantLock();
    try {
      lock.lock(); // Noncompliant [[flows=unlocking]] {{Unlock this lock along all executions paths of this method.}} flow@unlocking {{Lock 'lock' is never unlocked.}}
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
      lock.lock(); // Noncompliant [[flows=unlockingTry]] {{Unlock this lock along all executions paths of this method.}} flow@unlockingTry {{Lock 'lock' is never unlocked.}}
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
  
  private volatile ScheduledExecutorService executorService;
  private final Runnable task = new Task();

  @Override protected final void doStart() {
    executorService = MoreExecutors.renamingDecorator(executor(), new Supplier<String>() {
      @Override public String get() {
        return serviceName() + " " + state();
      }
    });
    executorService.execute(new Runnable() {
      @Override public void run() {
        lock.lock();
        try {
          startUp();
          runningTask = scheduler().schedule(delegate, executorService, task);
          notifyStarted();
        } catch (Throwable t) {
          notifyFailed(t);
          if (runningTask != null) {
            // prevent the task from running if possible
            runningTask.cancel(false);
          }
        } finally {
          lock.unlock();
        }
      }
    });
  }
  
  private final AbstractService delegate = new AbstractService() {
    private volatile Future<?> runningTask;
    private volatile ScheduledExecutorService executorService;
    private final ReentrantLock lock = new ReentrantLock();

    private final Runnable task = new Task();

    @Override protected final void doStartWithinDelegate() {
      executorService = MoreExecutors.executor();
      executorService.execute(new Runnable() {
        @Override public void run() {
          lock.lock();
          try {
            startUp();
          } finally {
            lock.unlock();
          }
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
  }
}
