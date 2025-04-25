package checks.spring;

import java.util.concurrent.Future;

abstract class TransactionalMethodVisibilityCheckSample_Spring6 {
  
  public interface C {
    @org.springframework.transaction.annotation.Transactional
    int bar(); // Compliant
  }

  @org.springframework.scheduling.annotation.Async
  protected Future<String> protectedAsyncMethod(){ // compliant
    return  null;
  }

  @org.springframework.scheduling.annotation.Async
  private Future<String> privateAsyncMethod(){ // Noncompliant {{Make this method non-"private" or remove the "@Async" annotation.}}
    return  null;
  }

  @org.springframework.transaction.annotation.Transactional
  protected void protectedTransactionalMethod() {} // Compliant


  @org.springframework.transaction.annotation.Transactional
  private Future<String> privateTransactionalMethod(){ // Noncompliant {{Make this method non-"private" or remove the "@Transactional" annotation.}}
    return  null;
  }
}
