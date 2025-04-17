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
  protected abstract Future<String> aMethod(); // Compliant

  @Async
  public Future<String> asyncMethod(){ // compliant
    return  null;
  }

  @Async
  Future<String> defaultVisibilityAsyncMethod(){ // Compliant
    return  null;
  }

  @Async
  protected Future<String> protectedVisibilityAsyncMethod(){ // Compliant
    return  null;
  }

  @Async
  private  Future<String> privateAsyncMethod(){ // Noncompliant {{Make this method non-"private" or remove the "@Async" annotation.}}
    return  null;
  }

  @org.springframework.transaction.annotation.Transactional
  public void publicTransactionalMethod() {} // Compliant

  @org.springframework.transaction.annotation.Transactional
  protected void protectedTransactionalMethod() {} // Compliant

  @org.springframework.transaction.annotation.Transactional
  void defaultVisibilityTransactionalMethod() {} // Compliant

  @org.springframework.transaction.annotation.Transactional
  private void privateTransactionalMethod() {} // Noncompliant {{Make this method non-"private" or remove the "@Transactional" annotation.}}
}
