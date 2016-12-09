import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyClass {

  public void thisReportsOnlyOneIssue() {
    Lock lock = new ReentrantLock();
    if(foo) {
      lock.lock();// Noncompliant
      ifStmt();
    } else {
      lock.lock();// Noncompliant
      elseStmt();
    }
    end();
  }

  public void fourIssues() {
    Lock lock = new ReentrantLock();
    Lock lock2 = new ReentrantLock();
    if(foo) {
      lock.lock();// Noncompliant
      ifStmt();
    } else {
      lock.lock();// Noncompliant
      elseStmt();
    }
    end();
    if(foo) {
      lock2.lock();// Noncompliant
      ifStmt();
    } else {
      lock2.lock();// Noncompliant
      elseStmt();
    }
    end();
  }

}
