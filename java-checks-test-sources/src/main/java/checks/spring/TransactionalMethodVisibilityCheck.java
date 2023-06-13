package checks.spring;

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
  
  @org.springframework.transaction.annotation.Transactional
  public void publicTransactionalMethod() {} // Compliant

  @org.springframework.transaction.annotation.Transactional
  protected void protectedTransactionalMethod() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation}}
  
  @org.springframework.transaction.annotation.Transactional
  void defaultVisibilityTransactionalMethod() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation}}
}
