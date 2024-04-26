package checks;

import java.io.IOException;

enum Level {
  WARN;
}
interface Log {
  void log(Level level, String s, Exception e);
}

public class InterruptedExceptionCheckSample {
  static final Log LOGGER = null;

  public void run1 () {
    try {
      while (true) {
        if (LOGGER != null) throw new IOException("");
        throw new InterruptedException("");
        
      }
    }catch (java.io.IOException e) {
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }catch (InterruptedException e) { // Noncompliant {{Either re-interrupt this method or rethrow the "InterruptedException" that can be caught here.}}
//          ^^^^^^^^^^^^^^^^^^^^^^
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
    } catch (InterruptedException | java.io.IOException e) { // Noncompliant {{Either re-interrupt this method or rethrow the "InterruptedException" that can be caught here.}}
//           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
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

  public void catchUnionType2 () {
    try {
      while (true) {
        // do stuff
      }
    } catch (InterruptedException | java.io.IOException e) { // Compliant - FP from SONARJAVA-4497
      unknownField.log(Level.WARN, "Interrupted!", e);
      throw e;
    }
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

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Noncompliant
      throw new NonExistentException();
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Noncompliant
      throw new CustomizedInterruptedException();
    }
  }
}
