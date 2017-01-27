import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyClass {

  public void thisReportsOnlyOneIssue() {
    Lock lock = new ReentrantLock();
    if(foo) {
      lock.lock();// Noncompliant [[flows=cache1]] {{Unlock this lock along all executions paths of this method.}} flow@cache1 {{Lock 'lock' is never unlocked.}}
      ifStmt();
    } else {
      lock.lock();// Noncompliant [[flows=cache2]] {{Unlock this lock along all executions paths of this method.}} flow@cache2 {{Lock 'lock' is never unlocked.}}
      elseStmt();
    }
    end();
  }

  public void fourIssues() {
    Lock lock = new ReentrantLock();
    Lock lock2 = new ReentrantLock();
    if(foo) {
      lock.lock();// Noncompliant [[flows=cache3]] {{Unlock this lock along all executions paths of this method.}} flow@cache3 {{Lock 'lock' is never unlocked.}}
      ifStmt();
    } else {
      lock.lock();// Noncompliant [[flows=cache4]] {{Unlock this lock along all executions paths of this method.}} flow@cache4 {{Lock 'lock' is never unlocked.}}
      elseStmt();
    }
    end();
    if(foo) {
      lock2.lock();// Noncompliant [[flows=cache5]] {{Unlock this lock along all executions paths of this method.}} flow@cache5 {{Lock 'lock2' is never unlocked.}}
      ifStmt();
    } else {
      lock2.lock();// Noncompliant [[flows=cache6]] {{Unlock this lock along all executions paths of this method.}} flow@cache6 {{Lock 'lock2' is never unlocked.}}
      elseStmt();
    }
    end();
  }

}
