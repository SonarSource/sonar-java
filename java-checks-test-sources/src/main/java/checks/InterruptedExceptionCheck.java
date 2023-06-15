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

  public void throwNewInterruptedExceptionFromCatch() throws InterruptedException {
    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Compliant
      throw new InterruptedException();
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Compliant
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
    } catch (CustomizedInterruptedException e) { // Compliant
      throw new InterruptedException();
    }

    try {
      throwsInterruptedException();
    } catch (CustomizedInterruptedException e) { // Compliant
      throw new CustomizedInterruptedException();
    }

    try {
      throwsInterruptedException();
    } catch (CustomizedInterruptedException e) { // Noncompliant, RuntimeException is not an InterruptedException
      throw new RuntimeException();
    }
  }

  public void throwNewInterruptedExceptionFromFunction() throws InterruptedException {

    try {
      throwsInterruptedException();
    } catch (CustomizedInterruptedException e) { // Compliant
      interruptByThrowingInterruptedExceptionL4();
    }

    try {
      throwsInterruptedException();
    } catch (CustomizedInterruptedException e) { // Compliant
      interruptByThrowingInterruptedExceptionL3();
    }

    try {
      throwsInterruptedException();
    } catch (CustomizedInterruptedException e) { // Compliant
      interruptByThrowingInterruptedExceptionL2();
    }

    try {
      throwsInterruptedException();
    } catch (CustomizedInterruptedException e) { // Noncompliant, too many levels
      interruptByThrowingInterruptedExceptionL1();
    }
  }

  public void throwNewCustomizedInterruptedExceptionFromFunction() throws InterruptedException {
    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Compliant
      interruptByThrowingCustomizedInterruptedExceptionL4();
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Compliant
      interruptByThrowingCustomizedInterruptedExceptionL3();
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Compliant
      interruptByThrowingCustomizedInterruptedExceptionL2();
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Noncompliant, too many levels
      interruptByThrowingCustomizedInterruptedExceptionL1();
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

    InterruptedException ie = new InterruptedException();
    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Compliant
      throw ie;
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Noncompliant, a RuntimeException is thrown.
      doSomething();
      throw getRuntimeException();
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Compliant
      throw getInterruptedException();
    }
  }

  public void cutControlFlow() throws InterruptedException {
    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Noncompliant, because neither foo nor bar belong to the control flow of this catch block.

      Object instance = new Object() {
        void foo() {
          Thread.currentThread().interrupt();
        }

        void bar() throws InterruptedException {
          throw new InterruptedException();
        }
      };
    }

    try {
      throwsInterruptedException();
    } catch (InterruptedException e) { // Noncompliant, because neither foo, nor bar belong to the control flow of this catch block.
      Runnable foo = () -> Thread.currentThread().interrupt();
      Action bar = () -> {
        throw new InterruptedException();
      };
    }
  }

  public void throwExceptionInTryDirectly() throws InterruptedException {
    try {
      throw new RuntimeException();
    } catch (Exception e) { // Compliant
    }

    try {
      throw new InterruptedException();
    } catch (Exception e) { // Noncompliant
    }

    try {
      InterruptedException ie = new InterruptedException();
      throw ie;
    } catch (Exception e) { // Noncompliant
    }

    try {
      throw getInterruptedException();
    } catch (Exception e) { // Noncompliant
    }
  }

  void falsePositivesSonarjava4406() {
    try {
      try {
        throwsInterruptedException();
      } catch (InterruptedException ie) { // Noncompliant, because interruption state is lost.
      }
    } catch (Exception e) { // Compliant, because inner try does not throw an InterruptedException
    }

    try {
      try {
        throwsInterruptedException();
      } catch (InterruptedException ie) { // Compliant
        Thread.currentThread().interrupt();
      }
    } catch (Exception e) { // Compliant, because inner try does not throw an InterruptedException
    }

    try {
      try {
        throwsInterruptedException();
      } catch (InterruptedException ie) { // Compliant
        throw new InterruptedException();
      }
    } catch (Exception e) { // Noncompliant, because inner try throws an InterruptedException
    }

    try {
      try {
        throwsInterruptedException();
      } catch (InterruptedException ie) { // Compliant
        throw new InterruptedException();
      }
    } catch (InterruptedException e) { // Noncompliant, because explicitly catch InterruptedException
    }

    try {
      try {
        throwsInterruptedException();
      } catch (RuntimeException ie) { // Compliant
      }
    } catch (Exception e) { // Noncompliant, because inner try may throw an InterruptedException
    }

  }

  public void throwInterruptedExceptionFromResourceList() throws InterruptedException {
    try {
      try(AutoCloseable autocloseable = getAutoCloseableThrowsInterruptedException()) {
        doSomething();
      } catch (IOException ie) {
        doSomething();
      }
    } catch (Exception e) { // Noncompliant
    }
  }

  public void throwInterruptedExceptionWithinThrowStatement() throws InterruptedException {
    try {
      throw getRuntimeExceptionThrowsInterruptedException();
    } catch (Exception e) { // Noncompliant
    }
  }


  public void catchUnionType1() throws IOException, InterruptedException {
    try {
      getRuntimeExceptionThrowsInterruptedException();
      getRuntimeExceptionThrowsIOException();
    } catch (InterruptedException | IOException e) { // Compliant - FP from SONARJAVA-4497
      doSomething();
      throw e;
    }
  }

  private AutoCloseable getAutoCloseableThrowsInterruptedException() throws InterruptedException {
    throw getInterruptedException();
  }

  private RuntimeException getRuntimeExceptionThrowsInterruptedException() throws InterruptedException {
    throw getInterruptedException();
  }
  private RuntimeException getRuntimeExceptionThrowsIOException() throws IOException {
    throw new IOException();
  }

  @FunctionalInterface
  public interface Action {
    void action() throws InterruptedException;
  }

  private static RuntimeException getRuntimeException() {
    return new RuntimeException();
  }

  private static InterruptedException getInterruptedException() {
    return new InterruptedException();
  }

  private static void doSomething() {
  }

  private static void interruptByThrowingInterruptedExceptionL1() throws InterruptedException {
    interruptByThrowingInterruptedExceptionL2();
  }

  private static void interruptByThrowingInterruptedExceptionL2() throws InterruptedException {
    interruptByThrowingInterruptedExceptionL3();
  }

  private static void interruptByThrowingInterruptedExceptionL3() throws InterruptedException {
    interruptByThrowingInterruptedExceptionL4();
  }

  private static void interruptByThrowingInterruptedExceptionL4() throws InterruptedException {
    throw new InterruptedException();
  }

  private static void interruptByThrowingCustomizedInterruptedExceptionL1() throws InterruptedException {
    interruptByThrowingCustomizedInterruptedExceptionL2();
  }

  private static void interruptByThrowingCustomizedInterruptedExceptionL2() throws InterruptedException {
    interruptByThrowingCustomizedInterruptedExceptionL3();
  }

  private static void interruptByThrowingCustomizedInterruptedExceptionL3() throws InterruptedException {
    interruptByThrowingCustomizedInterruptedExceptionL4();
  }

  private static void interruptByThrowingCustomizedInterruptedExceptionL4() throws InterruptedException {
    throw new CustomizedInterruptedException();
  }

  public static void throwsException() throws Exception {
    throw new Exception();
  }

  public static void throwsInterruptedException() throws InterruptedException {
    throw new InterruptedException();
  }

  private static class CustomizedInterruptedException extends InterruptedException {
  }

}
