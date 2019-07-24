import java.util.function.Consumer;

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
  private static synchronized Comparable<Object> foo() {
    return new Comparable<Object>() {
      @Override
      public int compareTo(Object o) {
        Thread.sleep(200);    // Compliant, this won't be invoked when the lock is held
        return 0;
      }
    };
  }

  public synchronized Consumer<Object> get() {
    return s -> {
      Thread.sleep(200);    // Compliant
    };
  }

  public synchronized Consumer<Object> get2() {
    return s -> {
      synchronized(new Object()) {
        Thread.sleep(200);    // Noncompliant [[sc=16;ec=21]] {{Replace the call to "Thread.sleep(...)" with a call to "wait(...)".}}
      }
    };
  }
}
