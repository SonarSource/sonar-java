package checks.spring;

import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

class TransactionalMethodVisibilityCheck {
  
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

  public static abstract class Inner {

    @Async
    protected abstract Future<String> aMethod(); // Noncompliant
  }

  @Async
  public Future<String> asyncMethod(){ // compliant
    return  null;
  }

  @Async
  private  Future<String> asyncMethodPrivate(){ // Noncompliant {{Make this method "public" or remove the "@Async" annotation}}
    return  null;
  }

  @org.springframework.transaction.annotation.Transactional
  public void publicTransactionalMethod() {} // Compliant

  @org.springframework.transaction.annotation.Transactional
  protected void protectedTransactionalMethod() {} // Noncompliant [[sc=18;ec=46]] {{Make this method "public" or remove the "@Transactional" annotation}}
  
  @org.springframework.transaction.annotation.Transactional
  void defaultVisibilityTransactionalMethod() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation}}
}
