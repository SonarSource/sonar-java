package checks.spring;

import java.io.IOException;
import java.sql.SQLException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
  public void processOrder(Order order) throws IOException, SQLException { // Noncompliant [[quickfixes=qf1,qf2]]
//            ^^^^^^^^^^^^
  // fix@qf1 {{Add rollbackFor attribute}}
  // edit@qf1 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = {java.io.IOException.class, java.sql.SQLException.class})}}
  // fix@qf2 {{Add rollbackFor = Exception.class}}
  // edit@qf2 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  }

  @Transactional
  public void importData() throws Exception { // Noncompliant [[quickfixes=qf3]]
//            ^^^^^^^^^^
  // fix@qf3 {{Add rollbackFor = Exception.class}}
  // edit@qf3 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  }

  @Transactional(timeout = 30)
  public void withOtherAttributes() throws SQLException { // Noncompliant
//            ^^^^^^^^^^^^^^^^^^^
  }

  @Transactional
  public void customException() throws CustomCheckedException { // Noncompliant [[quickfixes=qf7,qf8]]
//            ^^^^^^^^^^^^^^^
  // fix@qf7 {{Add rollbackFor attribute}}
  // edit@qf7 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = checks.spring.CustomCheckedException.class)}}
  // fix@qf8 {{Add rollbackFor = Exception.class}}
  // edit@qf8 [[sl=-1;sc=3;el=-1;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  }

  @Transactional
  public void mixedExceptions() throws IOException, RuntimeException { // Noncompliant [[quickfixes=qf9,qf10]]
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

  @Transactional // [[secondary=+2]]
  static class ClassLevelNoConfig {
    public void noConfig() throws IOException { // Noncompliant {{Specify rollback behavior for checked exceptions using "rollbackFor" or "noRollbackFor" attributes on the class-level @Transactional.}}
//              ^^^^^^^^
    }

    @Transactional(rollbackFor = IOException.class)
    public void methodOverrides() throws IOException {
    }
  }

  @Transactional
  public void errorNotChecked() throws Error {
  }

  @org.springframework.transaction.annotation.Transactional
  public void fullyQualified() throws IOException { // Noncompliant [[quickfixes=qf13,qf14]]
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
  public void withValueAttribute() throws IOException { // Noncompliant
//            ^^^^^^^^^^^^^^^^^^
  }

  // Test nested structure to ensure parent traversal works
  static class OuterClass {
    @Transactional // [[secondary=+2]]
    static class InnerClassWithAnnotation {
      public void nestedMethod() throws IOException { // Noncompliant {{Specify rollback behavior for checked exceptions using "rollbackFor" or "noRollbackFor" attributes on the class-level @Transactional.}}
//                ^^^^^^^^^^^^
      }
    }

    static class InnerClassNoAnnotation {
      public void nestedMethodNoAnnotation() throws IOException {
        // No @Transactional at any level, so no issue
      }
    }
  }

  @Transactional // [[secondary=+2]]
  interface TransactionalInterface {
    void interfaceMethod() throws IOException; // Noncompliant {{Specify rollback behavior for checked exceptions using "rollbackFor" or "noRollbackFor" attributes on the class-level @Transactional.}}
//       ^^^^^^^^^^^^^^^
  }

  // Test meta-annotated (composed) annotation
  @MyTransactional
  public void metaAnnotated() throws IOException { // Noncompliant
//            ^^^^^^^^^^^^^
  }

  // Test annotation with value attribute (transaction manager name)
  @Transactional("txManager")
  public void valueShorthand() throws IOException { // Noncompliant
//            ^^^^^^^^^^^^^^
    // Has value attribute but no rollback configuration
  }
}
