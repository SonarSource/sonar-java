package checks;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.function.LongSupplier;

enum Level {
  WARN;
}
interface Log {
  void log(Level level, String s, Throwable e);
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
        if (LOGGER != null) throw new IOException("");
        throw new InterruptedException("");
      }
    }catch (java.io.IOException e) {
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }catch (InterruptedException e) { // Noncompliant
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }
  }

  public void catchUnionType () {
    try {
      while (true) {
        if (LOGGER != null) throw new IOException("");
        throw new InterruptedException("");
      }
    } catch (InterruptedException | java.io.IOException e) { // Noncompliant [[sc=14;ec=58]] {{Either re-interrupt this method or rethrow the "InterruptedException" that can be caught here.}}
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

  public Object getNextTask(BlockingQueue<Object> queue) {
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

}

class Interruptable {
  static final Log LOGGER = null;

  void interrupt() {
    
  }

  private static void waitForNextExecution(Set<Runnable> running, LongSupplier waitTimeoutMillis) {
    try {
      Thread.sleep(waitTimeoutMillis.getAsLong());
    } catch (InterruptedException e) { //Compliant
      cancelAllSubTasksAndInterrupt(running); 
    }
  }

  private static void cancelAllSubTasksAndInterrupt(Set<Runnable> subTasks) {
    for (Runnable task : subTasks) {
      System.out.println("--- waitForNextExecution: Service interrupted. Cancel execution of task {}.");
    }
    Thread.currentThread().interrupt();
  }
  
  private static void waitForNextExecution1(Set<Runnable> running, LongSupplier waitTimeoutMillis) {
    try {
      Thread.sleep(waitTimeoutMillis.getAsLong());
    } catch (InterruptedException e) { // Noncompliant, too many levels
      cancelAllSubTasksAndInterrupt1(running); 
    }
  }

  private static void cancelAllSubTasksAndInterrupt1(Set<Runnable> subTasks) {
    cancelAllSubTasksAndInterrupt2(subTasks);
  }

  private static void cancelAllSubTasksAndInterrupt2(Set<Runnable> subTasks) { 
    cancelAllSubTasksAndInterrupt3(subTasks);
  }

  private static void cancelAllSubTasksAndInterrupt3(Set<Runnable> subTasks) {
    cancelAllSubTasksAndInterrupt4(subTasks);
    if (LOGGER != null )throw new RuntimeException();
  }
  
  private static void cancelAllSubTasksAndInterrupt4(Set<Runnable> subTasks) {
    for (Runnable task : subTasks) {
      System.out.println("--- waitForNextExecution: Service interrupted. Cancel execution of task {}.");
    }
    Thread.currentThread().interrupt();
  }

  public void catchGenericException() throws InterruptedException {
    try {
      throwsInterruptedException();
    } catch (Exception e) { // Noncompliant [[sc=14;ec=25;secondary=-1]] {{Either re-interrupt this method or rethrow the "InterruptedException" that can be caught here.}}
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }

    try {
      throwsInterruptedException();
      throwsException();
    } catch (Exception e) { // Noncompliant [[sc=14;ec=25;secondary=-2]]
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }

    try {
      throwsInterruptedException();
    } catch (Throwable e) { // Noncompliant {{Either re-interrupt this method or rethrow the "InterruptedException" that can be caught here.}}
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }

    try {
      throwsInterruptedException();
    } catch (Exception e) { // Compliant, correct handling
      Thread.currentThread().interrupt();
    }

    try {
      throwsInterruptedException();
    } catch (IllegalArgumentException e) { // Compliant, InterruptedException not caught
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }

    try {
      throwsException();
    } catch (Exception e) { // Compliant
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }
  }

  public void catchMultiplesException() throws InterruptedException {
    try {
      throwsInterruptedException();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) { // Compliant, correctly handled in the previous catch
      LOGGER.log(Level.WARN, "Something else!", e);
    }

    try {
      throwsInterruptedException();
    } catch (Exception e) {
      Thread.currentThread().interrupt();
    } catch (Throwable e) { // Compliant, correctly handled in the previous catch
      LOGGER.log(Level.WARN, "Something else!", e);
    }

    try {
      throwsInterruptedException();
    } catch (Exception e) { // Noncompliant
      LOGGER.log(Level.WARN, "Something else!", e);
    } catch (Throwable e) {
      Thread.currentThread().interrupt();
    }
  }

  public void throwsException() throws Exception {
    throw new Exception();
  }

  public void throwsInterruptedException() throws InterruptedException {
    throw new InterruptedException();
  }

}
