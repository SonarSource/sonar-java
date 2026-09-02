package checks.spring;

import java.io.IOException;
import java.sql.SQLException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Composed/meta-annotation for testing
@Retention(RetentionPolicy.RUNTIME)
@Transactional
@interface MyTransactional {
}

class Order {}
class NotificationException extends Exception {}
class CustomCheckedException extends Exception {}

public class TransactionalMethodCheckedExceptionCheckSample {

  @Transactional
  public void processOrder(Order order) throws IOException, SQLException { // Noncompliant [[secondary=22;quickfixes=qf1,qf2]]
//            ^^^^^^^^^^^^
  // fix@qf1 {{Add rollbackFor attribute}}
  // edit@qf1 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = {java.io.IOException.class, java.sql.SQLException.class})}}
  // fix@qf2 {{Add rollbackFor = Exception.class}}
  // edit@qf2 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  }

  @Transactional
  public void importData() throws Exception { // Noncompliant [[secondary=31;quickfixes=qf3]]
//            ^^^^^^^^^^
  // fix@qf3 {{Add rollbackFor = Exception.class}}
  // edit@qf3 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  }

  @Transactional(timeout = 30)
  public void withOtherAttributes() throws SQLException { // Noncompliant [[secondary=38]]
//            ^^^^^^^^^^^^^^^^^^^
  }

  @Transactional
  public void customException() throws CustomCheckedException { // Noncompliant [[secondary=43;quickfixes=qf7,qf8]]
//            ^^^^^^^^^^^^^^^
  // fix@qf7 {{Add rollbackFor attribute}}
  // edit@qf7 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = checks.spring.CustomCheckedException.class)}}
  // fix@qf8 {{Add rollbackFor = Exception.class}}
  // edit@qf8 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  }

  @Transactional
  public void mixedExceptions() throws IOException, RuntimeException { // Noncompliant [[secondary=52;quickfixes=qf9,qf10]]
//            ^^^^^^^^^^^^^^^
  // fix@qf9 {{Add rollbackFor attribute}}
  // edit@qf9 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = java.io.IOException.class)}}
  // fix@qf10 {{Add rollbackFor = Exception.class}}
  // edit@qf10 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  }

  @Transactional(rollbackFor = IOException.class)
  public void withRollbackFor() throws IOException {
  }

  @Transactional(rollbackFor = {IOException.class, SQLException.class})
  public void withMultipleRollbackFor() throws IOException, SQLException {
  }

  @Transactional(rollbackFor = Exception.class)
  public void rollbackForAll() throws Exception {
  }

  @Transactional(noRollbackFor = NotificationException.class)
  public void withNoRollbackFor() throws NotificationException {
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = NotificationException.class)
  public void withBoth() throws Exception {
  }

  @Transactional(rollbackForClassName = "java.io.IOException")
  public void rollbackForClassName() throws IOException {
  }

  @Transactional(noRollbackForClassName = "checks.spring.NotificationException")
  public void noRollbackForClassName() throws NotificationException {
  }

  @Transactional
  public void noExceptions() {
  }

  @Transactional
  public void uncheckedOnly() throws RuntimeException {
  }

  public void noAnnotation() throws IOException {
  }

  @Transactional(rollbackFor = Exception.class)
  static class ClassLevelConfig {
    public void inherited() throws IOException {
    }
  }

  @Transactional
  static class ClassLevelNoConfig {
    public void noConfig() throws IOException { // Compliant - method name does not suggest a transactional operation
    }

    public void saveOrder() throws IOException { // Noncompliant [[secondary=108]]
//              ^^^^^^^^^
    }

    public void deleteRecord() throws IOException { // Noncompliant [[secondary=108]]
//              ^^^^^^^^^^^^
    }

    public void updateStatus() throws IOException { // Noncompliant [[secondary=108]]
//              ^^^^^^^^^^^^
    }

    public void persistEntity() throws IOException { // Noncompliant [[secondary=108]]
//              ^^^^^^^^^^^^^
    }

    public void mergeData() throws IOException { // Noncompliant [[secondary=108]]
//              ^^^^^^^^^
    }

    public void flushChanges() throws IOException { // Noncompliant [[secondary=108]]
//              ^^^^^^^^^^^^
    }

    public void readData() throws IOException { // Compliant - no transactional prefix
    }

    @Transactional(rollbackFor = IOException.class)
    public void methodOverrides() throws IOException {
    }
  }

  @Transactional
  public void errorNotChecked() throws Error {
  }

  @org.springframework.transaction.annotation.Transactional
  public void fullyQualified() throws IOException { // Noncompliant [[secondary=121;quickfixes=qf13,qf14]]
//            ^^^^^^^^^^^^^^
  // fix@qf13 {{Add rollbackFor attribute}}
  // edit@qf13 [[sl=-1;sc=3;el=-1;ec=60]] {{@org.springframework.transaction.annotation.Transactional(rollbackFor = java.io.IOException.class)}}
  // fix@qf14 {{Add rollbackFor = Exception.class}}
  // edit@qf14 [[sl=-1;sc=3;el=-1;ec=60]] {{@org.springframework.transaction.annotation.Transactional(rollbackFor = java.lang.Exception.class)}}
  }

  @Transactional(rollbackFor = IOException.class)
  public void partialConfig() throws SQLException {
  }

  @Transactional(value = "txManager")
  public void withValueAttribute() throws IOException { // Noncompliant [[secondary=134]]
//            ^^^^^^^^^^^^^^^^^^
  }

  // Test nested structure to ensure parent traversal works
  static class OuterClass {
    @Transactional
    static class InnerClassWithAnnotation {
      public void nestedMethod() throws IOException { // Compliant - no transactional prefix
      }

      public void saveNestedEntity() throws IOException { // Noncompliant [[secondary=155]]
//                ^^^^^^^^^^^^^^^^
      }
    }

    static class InnerClassNoAnnotation {
      public void nestedMethodNoAnnotation() throws IOException {
        // No @Transactional at any level, so no issue
      }
    }
  }

  @Transactional
  interface TransactionalInterface {
    void interfaceMethod() throws IOException; // Compliant - no transactional prefix

    void saveEntity() throws IOException; // Noncompliant [[secondary=169]]
//       ^^^^^^^^^^ {{Specify rollback behavior for checked exceptions using "rollbackFor" or "noRollbackFor" attributes on the class-level @Transactional.}}
  }

  // Test meta-annotated (composed) annotation
  @MyTransactional
  public void metaAnnotated() throws IOException { // Noncompliant [[secondary=162]]
//            ^^^^^^^^^^^^^
  }

  // Test annotation with value attribute (transaction manager name)
  @Transactional("txManager")
  public void valueShorthand() throws IOException { // Noncompliant [[secondary=168]]
//            ^^^^^^^^^^^^^^
    // Has value attribute but no rollback configuration
  }

  // @Transactional has no effect on non-public methods (Spring proxy-based AOP only intercepts public methods)
  @Transactional
  private void privateMethod() throws IOException { // Compliant - private methods are not proxied by Spring
  }

  @Transactional
  private void privateMethodMultipleExceptions() throws IOException, SQLException { // Compliant
  }

  @Transactional
  protected void protectedMethod() throws IOException { // Compliant - protected methods are not proxied by Spring
  }

  @Transactional
  void packagePrivateMethod() throws IOException { // Compliant - package-private methods are not proxied by Spring
  }

  @Transactional
  static class ClassLevelWithNonPublicMethods {
    private void privateInClassLevel() throws IOException { // Compliant - private methods are not proxied
    }

    protected void protectedInClassLevel() throws IOException { // Compliant - protected methods are not proxied
    }

    void packagePrivateInClassLevel() throws IOException { // Compliant - package-private methods are not proxied
    }

    public void publicInClassLevel() throws IOException { // Compliant - no transactional prefix
    }

    public void savePublicInClassLevel() throws IOException { // Noncompliant [[secondary=207]]
//              ^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  // Compliant: propagation = NOT_SUPPORTED means no transaction is created
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void notSupported() throws IOException { // Compliant
  }

  // Compliant: propagation = NEVER means no transaction is created
  @Transactional(propagation = Propagation.NEVER)
  public void neverPropagation() throws IOException { // Compliant
  }

  // Compliant: readOnly = true means no writes can occur
  @Transactional(readOnly = true)
  public void readOnlyTransaction() throws IOException { // Compliant
  }

  // Compliant: combination of readOnly and NOT_SUPPORTED
  @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
  public void readOnlyAndNotSupported() throws IOException { // Compliant
  }

  // readOnly = false with default propagation is still noncompliant
  @Transactional(readOnly = false)
  public void readOnlyFalse() throws IOException { // Noncompliant [[secondary=228]]
//            ^^^^^^^^^^^^^
  }

  // Explicit REQUIRED propagation still needs rollback config
  @Transactional(propagation = Propagation.REQUIRED)
  public void requiredPropagation() throws IOException { // Noncompliant [[secondary=234]]
//            ^^^^^^^^^^^^^^^^^^^
  }

  // Compliant: NOT_SUPPORTED with other attributes
  @Transactional(propagation = Propagation.NOT_SUPPORTED, timeout = 30)
  public void notSupportedWithTimeout() throws IOException { // Compliant
  }

  // Compliant: readOnly with other attributes
  @Transactional(readOnly = true, timeout = 30)
  public void readOnlyWithTimeout() throws IOException { // Compliant
  }

  // Compliant: static-imported NOT_SUPPORTED propagation (identifier without member select)
  @Transactional(propagation = NOT_SUPPORTED)
  public void notSupportedStaticImport() throws IOException { // Compliant
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  static class ClassLevelNotSupported {
    public void methodInNotSupportedClass() throws IOException { // Compliant
    }
  }

  @Transactional(readOnly = true)
  static class ClassLevelReadOnly {
    public void methodInReadOnlyClass() throws IOException { // Compliant
    }
  }

  // Method with non-Transactional annotations, class-level @Transactional found after iterating class annotations
  @Deprecated
  @Transactional
  static class ClassWithMultipleAnnotations {
    @Deprecated
    @SuppressWarnings("unused")
    public void methodWithOtherAnnotations() throws IOException { // Compliant - no transactional prefix
    }

    @Deprecated
    public void deleteWithAnnotations() throws IOException { // Noncompliant [[secondary=290]]
//              ^^^^^^^^^^^^^^^^^^^^^
    }
  }

  @Transactional
  static class ClassLevelWithMethodCalls {
    public void processOrder(Object repository) throws IOException { // Noncompliant [[secondary=297]]
//              ^^^^^^^^^^^^
      repository.toString();
      saveOrder();
    }

    public void doWork() throws IOException { // Compliant - no transactional prefix and no transactional calls
      System.out.println("work");
    }

    private void saveOrder() {
    }

    public void delegateToFlush() throws IOException { // Noncompliant [[secondary=297]]
//              ^^^^^^^^^^^^^^^
      flushAll();
    }

    private void flushAll() {
    }

    public void delegateToDelete(Object entity) throws IOException { // Noncompliant [[secondary=297]]
//              ^^^^^^^^^^^^^^^^
      deleteEntity(entity);
    }

    private void deleteEntity(Object entity) {
    }

    public void noTransactionalCalls() throws IOException { // Compliant - no transactional method calls
      System.out.println("no transaction");
      computeResult();
    }

    private void computeResult() {
    }
  }
}
