import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.logging.Logger;

class A {
  Object monitor;
  void foo() {
    Thread.sleep(12);
    Thread.sleep(12, 13);
    synchronized(monitor) {
      while(notReady()){
        Thread.sleep(200);    // Noncompliant [[sc=16;ec=21]] {{Replace the call to "Thread.sleep(...)" with a call to "wait(...)".}}
        Thread.sleep(200, 12); // Noncompliant
      }
      process();
    }
  }

  synchronized void foo() {
    Thread.sleep(200);    // Noncompliant
    Thread.sleep(200, 12); // Noncompliant
  }
}

public class Testcase {
  private static final Logger LOGGER = Logger.getLogger("com.reftel.magnus.sqsynchtest");

  public synchronized Consumer<Semaphore> get() {
    return s -> {
      LOGGER.info("About to wait");
      s.release();
      try {
        Thread.sleep(3000); // Compliant: this won't be invoked when the lock is held
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
      LOGGER.info("Done waiting");
    };
  }

  private static synchronized Comparable<Object> foo() {
    return new Comparable<Object>() {
      @Override
      public int compareTo(Object o) {
        Thread.sleep(200);    // Compliant, this won't be invoked when the lock is held
      }
    };
  }

  public synchronized void print() {
    LOGGER.info("Running print()");
  }

  public static void main(String[] args) throws Exception {
    Testcase a = new Testcase();
    Semaphore s = new Semaphore(0);
    Consumer<Semaphore> c = a.get();
    Thread t = new Thread(() -> c.accept(s));
    t.start();
    s.acquire();
    a.print();
    t.join();
  }

  public synchronized Consumer<Semaphore> get() {
    return s -> {
      synchronized(monitor) {
        Thread.sleep(200);    // Noncompliant [[sc=16;ec=21]] {{Replace the call to "Thread.sleep(...)" with a call to "wait(...)".}}
      }
    };
  }

}
