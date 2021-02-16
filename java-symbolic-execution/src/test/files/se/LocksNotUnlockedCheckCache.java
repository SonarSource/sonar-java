import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyClass {

  public void thisReportsOnlyOneIssue() {
    Lock lock = new ReentrantLock();
    if(foo) {
      lock.lock();// Noncompliant {{Unlock this lock along all executions paths of this method.}}
      ifStmt();
    } else {
      lock.lock();// Noncompliant {{Unlock this lock along all executions paths of this method.}}
      elseStmt();
    }
    end();
  }

  public void fourIssues() {
    Lock lock = new ReentrantLock();
    Lock lock2 = new ReentrantLock();
    if(foo) {
      lock.lock();// Noncompliant {{Unlock this lock along all executions paths of this method.}}
      ifStmt();
    } else {
      lock.lock();// Noncompliant {{Unlock this lock along all executions paths of this method.}}
      elseStmt();
    }
    end();
    if(foo) {
      lock2.lock();// Noncompliant {{Unlock this lock along all executions paths of this method.}}
      ifStmt();
    } else {
      lock2.lock();// Noncompliant  {{Unlock this lock along all executions paths of this method.}}
      elseStmt();
    }
    end();
  }

}
