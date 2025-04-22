package checks.spring;

import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

abstract class TransactionalMethodVisibilityCheckSample {
  
  public interface C {
    @Transactional
    int bar(); // Compliant
  }

  @Async
  public Future<String> asyncMethod(){ // compliant
    return  null;
  }

  @Async
  private  Future<String> privateAsyncMethod(){ // Noncompliant {{Make this method "public" or remove the "@Async" annotation.}}
    return  null;
  }

  @org.springframework.transaction.annotation.Transactional
  public void publicTransactionalMethod() {} // Compliant
}
