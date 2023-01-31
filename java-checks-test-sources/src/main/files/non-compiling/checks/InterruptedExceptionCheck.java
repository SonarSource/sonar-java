package checks;

import java.io.IOException;

enum Level {
  WARN;
}
interface Log {
  void log(Level level, String s, Exception e);
}

public class InterruptedExceptionCheck {
  static final Log LOGGER = null;

  public void run1 () {
    try {
      while (true) {
        if (LOGGER != null) throw new IOException("");
        throw new InterruptedException("");
        
      }
    }catch (java.io.IOException e) {
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }catch (InterruptedException e) { // Noncompliant [[sc=13;ec=35]] {{Either re-interrupt this method or rethrow the "InterruptedException" that can be caught here.}}
        LOGGER.log(Level.WARN, "Interrupted!", e);
    }
  }

  public void runUnknownSymbol () {
    try {
      while (true) {
        // do stuff
      }
    }catch (java.io.IOException e) {
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }catch (InterruptedException e) { // Noncompliant
      unknownField.log(Level.WARN, "Interrupted!", e);
    }
  }

  public void catchUnionType () {
    try {
      while (true) {
        // do stuff
      }
    } catch (InterruptedException | java.io.IOException e) { // Noncompliant [[sc=14;ec=58]] {{Either re-interrupt this method or rethrow the "InterruptedException" that can be caught here.}}
      unknownField.log(Level.WARN, "Interrupted!", e);
    }
  }

  public void run () throws InterruptedException{
    try {
      while (true) {
        // do stuff
      }
    } catch (InterruptedException e) {
        LOGGER.log(Level.WARN, "Interrupted!", e);
        // clean up state...
        throw e;
    }
    try {
    } catch (InterruptedException e) { // Noncompliant {{Either re-interrupt this method or rethrow the "InterruptedException" that can be caught here.}}
      LOGGER.log(Level.WARN, "Interrupted!", e);
      throw new java.io.IOException();
    }
    try {
    } catch (InterruptedException e) { // Noncompliant {{Either re-interrupt this method or rethrow the "InterruptedException" that can be caught here.}}
      LOGGER.log(Level.WARN, "Interrupted!", e);
      Exception e1 = new Exception();
      throw e1;
    }
    try {
    } catch (InterruptedException e) { // Noncompliant {{Either re-interrupt this method or rethrow the "InterruptedException" that can be caught here.}}
      LOGGER.log(Level.WARN, "Interrupted!", e);
      throw new IllegalStateException("foo", e);
    }
    try {
    } catch (ThreadDeath threadDeath) {
      throw threadDeath;
    }
    try {
    } catch (ThreadDeath threadDeath) { // Noncompliant {{Either re-interrupt this method or rethrow the "ThreadDeath" that can be caught here.}}
      throw new java.io.IOException();
   }
  }

  void wrongExceptionOrder() {
    // Does not compile ("Exception is already caught")
    try {
      throwsInterruptedException();
    } catch (Exception e) { // Noncompliant
      LOGGER.log(Level.WARN, "Interrupted!", e);
    } catch (InterruptedException e) {
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }

    try {
      throwsInterruptedException();
    } catch (Exception e) { // Noncompliant
      LOGGER.log(Level.WARN, "Interrupted!", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    try {
      throwsInterruptedException();
    } catch (Exception e) { // Compliant
      Thread.currentThread().interrupt();
    } catch (InterruptedException e) {
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }
  }

  public void runInterrupted() {
    try {
      throw  new InterruptedException();
    } catch (InterruptedException e) {
        LOGGER.log(Level.WARN, "Interrupted!", e);
        // clean up state...
        Thread.currentThread().interrupt();
      }
    try {
      while (true) {
        throw  new InterruptedException();
        // do stuff
      }
    } catch (InterruptedException e) { // Noncompliant {{Either re-interrupt this method or rethrow the "InterruptedException" that can be caught here.}}
      LOGGER.log(Level.WARN, "Interrupted!", e);
      // clean up state...
      new Interruptable().interrupt();
    }
  }

  public Task getNextTask(BlockingQueue<Task> queue) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          return queue.take();
        } catch (InterruptedException e) {
          interrupted = true;
          // fall through and retry
        }
      }
    } finally {
      if (interrupted)
        Thread.currentThread().interrupt();
    }
  }

  public void throwsInterruptedException() throws InterruptedException {
    throw new InterruptedException();
  }

}

class Interruptable {
  void interrupt() {
    
  }

  private static void waitForNextExecution2(Set<Runnable> running, LongSupplier waitTimeoutMillis) throws InterruptedException {
    try {
      Thread.sleep(waitTimeoutMillis.getAsLong());
    } catch (InterruptedException e) { //Compliant
      throw new InterruptedException();
      cancelAllSubTasksAndInterrupt555(running);
      throw new IOException();
      cancelAllSubTasksAndInterrupt555(running);
      cancelAllSubTasksAndInterrupt555(running);
    }
  }

  private static void cancelAllSubTasksAndInterrupt555(Set<Runnable> subTasks) {
    for (Runnable task : subTasks) {
      System.out.println("--- waitForNextExecution: Service interrupted. Cancel execution of task {}.");
    }
    Thread.currentThread().interrupt();
  }

  public void throwNewInterruptedExceptionFromCatch() throws InterruptedException {
    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Compliant
      throw new InterruptedException();
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Noncompliant, unknown exception type is not regarded an InterruptedException
      throw new NonExistentException();
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Noncompliant, unknown supertype of exception is not regarded an InterruptedException
      throw new CustomizedInterruptedException();
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Noncompliant, RuntimeException is not an InterruptedException
      throw new RuntimeException();
    }
  }


  public void catchSubtypeOfInterruptedException() throws InterruptedException {
    try {
      throwsInterruptedException();
    } catch (CustomizedInterruptedException e) { // Compliant, unknown supertype of catched exception is not regarded an InterruptedException
      throw new InterruptedException();
    }

    try {
      throwsInterruptedException();
    } catch (CustomizedInterruptedException e) { // Compliant, unknown supertype of catched exception is not regarded an InterruptedException
      throw new CustomizedInterruptedException();
    }

    try {
      throwsInterruptedException();
    } catch (CustomizedInterruptedException e) { // Compliant, unknown supertype of catched exception is not regarded an InterruptedException
      throw new RuntimeException();
    }
  }

  public void throwNewInterruptedExceptionFromFunction() throws InterruptedException {

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Noncompliant, unknown function is not regarded interrupting this thread
      nonExistingFunction();
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Compliant, known function that interrupts this thread
      interruptByThrowingInterruptedException();
    }
  }

  public void rethrowSameException() throws InterruptedException {

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Compliant
      doSomething();
      throw e;
    }

    RuntimeException re = new RuntimeException();
    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Noncompliant, re is not e
      doSomething();
      throw re;
    }
  }

  private static void interruptByThrowingInterruptedException() throws InterruptedException {
    throw new InterruptedException();
  }

  private static class CustomizedInterruptedException extends NonExistentException {
  }

}
