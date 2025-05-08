package checks.spring;

import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

abstract class TransactionalMethodVisibilityCheckSample_Spring5 {
  
  public interface C {
    @Transactional
    int bar(); // Compliant
  }
  
  @Async
  protected abstract Future<String> aMethod(); // Noncompliant

  @Async
  Future<String> defaultVisibilityAsyncMethod(){ // Noncompliant
    return  null;
  }

  @Async
  protected Future<String> protectedVisibilityAsyncMethod(){ // Noncompliant
    return  null;
  }

  @org.springframework.transaction.annotation.Transactional
  protected void protectedTransactionalMethod() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation.}}
//               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  @org.springframework.transaction.annotation.Transactional
  void defaultVisibilityTransactionalMethod() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation.}}
}
