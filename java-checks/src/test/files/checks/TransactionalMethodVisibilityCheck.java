import org.springframework.transaction.annotation.Transactional;

class A {
  
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
  private void privateTransactionalMethod() {} // Noncompliant [[sc=16;ec=42]] {{Make this method "public" or remove the "@Transactional" annotation}}

  @org.springframework.transaction.annotation.Transactional
  protected void protectedTransactionalMethod() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation}}
  
  @org.springframework.transaction.annotation.Transactional
  void defaultVisibilityTransactionalMethod() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation}}

  @org.xxx.Transactional
  private void privateMethodWithNonSpringAnnotation() {} // Compliant

  @Transactional
  private void privateTransactionalMethodWithImportBasedAnnotation() {} // Noncompliant {{Make this method "public" or remove the "@Transactional" annotation}}
  
}
