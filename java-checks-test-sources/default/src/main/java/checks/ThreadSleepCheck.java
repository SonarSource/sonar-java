package checks;

import java.util.Random;
import java.util.function.Consumer;

class ThreadSleepCheck {
  class A {
    Object monitor;

    void foo() throws InterruptedException {
      Thread.sleep(12);
      Thread.sleep(12, 13);
      synchronized (monitor) {
        while (notReady()) {
          Thread.sleep(200);    // Noncompliant [[sc=18;ec=23]] {{Replace the call to "Thread.sleep(...)" with a call to "wait(...)".}}
          Thread.sleep(200, 12); // Noncompliant
        }
        process();
      }
    }

    private void process() {
    }

    private boolean notReady() {
      return (new Random()).nextBoolean();
    }

    synchronized void foo2() throws InterruptedException {
      Thread.sleep(200);    // Noncompliant
      Thread.sleep(200, 12); // Noncompliant
    }
  }

  public class Testcase {
    private static synchronized Comparable<Object> foo() {
      return new Comparable<Object>() {
        @Override
        public int compareTo(Object o) {
          try {
            Thread.sleep(200);    // Compliant, this won't be invoked when the lock is held
          } catch (InterruptedException e) {
            e.printStackTrace();
          } finally {
            return 0;
          }
        }
      };
    }

    public synchronized Consumer<Object> get() {
      return s -> {
        try {
          Thread.sleep(200);    // Compliant
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      };
    }

    public synchronized Consumer<Object> get2() {
      return s -> {
        synchronized (new Object()) {
          try {
            Thread.sleep(200);    // Noncompliant [[sc=20;ec=25]] {{Replace the call to "Thread.sleep(...)" with a call to "wait(...)".}}
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      };
    }
  }
}
