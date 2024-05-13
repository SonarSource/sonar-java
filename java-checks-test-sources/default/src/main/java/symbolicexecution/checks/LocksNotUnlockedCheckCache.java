package symbolicexecution.checks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LocksNotUnlockedCheckCache {

  public void thisReportsOnlyOneIssue(boolean foo) {
    Lock lock = new ReentrantLock();
    if(foo) {
      lock.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
      ifStmt();
    } else {
      lock.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
      elseStmt();
    }
    end();
  }

  public void fourIssues(boolean foo) {
    Lock lock = new ReentrantLock();
    Lock lock2 = new ReentrantLock();
    if(foo) {
      lock.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
      ifStmt();
    } else {
      lock.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
      elseStmt();
    }
    end();
    if(foo) {
      lock2.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
      ifStmt();
    } else {
      lock2.lock(); // Noncompliant {{Unlock this lock along all executions paths of this method.}}
      elseStmt();
    }
    end();
  }

  private void end() {
  }

  private void ifStmt() {
  }

  private void elseStmt() {
  }
}
