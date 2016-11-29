import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyClass {

  public void thisReportsOnlyOneIssue() {
    Lock lock = new ReentrantLock();
    if(foo) {
      lock.lock();// Noncompliant
    } else {
      lock.lock();// Noncompliant
    }
  }

}
