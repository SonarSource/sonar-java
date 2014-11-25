import org.springframework.transaction.annotation.Transactional;

class A {
  
  @org.springframework.transaction.annotation.Transactional
  public void publicTransactionalMethod() {}

  @org.springframework.transaction.annotation.Transactional
  private void privateTransactionalMethod() {}

  @org.springframework.transaction.annotation.Transactional
  protected void protectedTransactionalMethod() {}
  
  @org.springframework.transaction.annotation.Transactional
  void defaultVisibilityTransactionalMethod() {}

  @org.xxx.Transactional
  private void privateMethodWithNonSpringAnnotation() {}

  @Transactional
  private void privateTransactionalMethodWithImportBasedAnnotation() {}
  
}