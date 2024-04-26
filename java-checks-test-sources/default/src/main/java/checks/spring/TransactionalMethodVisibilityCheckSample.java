package checks.spring;

import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

abstract class TransactionalMethodVisibilityCheckSample {
  
  public interface C {
    @Transactional
    int bar(); // Compliant
  }
  
  private interface B {
    @Transactional
    int bar(); // Compliant
  }

  private interface A {
    @Async
    int bar(); // Compliant
  }

  @Async
  protected abstract Future<String> aMethod(); // Noncompliant


  @Async
  public Future<String> asyncMethod(){ // compliant
    return  null;
  }

  @Async
  private  Future<String> asyncMethodPrivate(){ // Noncompliant {{Make this method "public" or remove the "@Async" annotation.}}
    return  null;
  }

  @org.springframework.transaction.annotation.Transactional
  public void publicTransactionalMethod() {} // Compliant

  @org.springframework.transaction.annotation.Transactional
  protected void protectedTransactionalMethod() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation.}}
//               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  
  @org.springframework.transaction.annotation.Transactional
  void defaultVisibilityTransactionalMethod() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation.}}
}
