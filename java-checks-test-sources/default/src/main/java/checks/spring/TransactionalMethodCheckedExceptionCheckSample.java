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

  @Transactional // Noncompliant {{Specify rollback behavior for checked exceptions using "rollbackFor" or "noRollbackFor" attributes.}} [[quickfixes=qf1,qf2]]
//^^^^^^^^^^^^^^
  // fix@qf1 {{Add rollbackFor attribute}}
  // edit@qf1 [[sc=3;ec=17]] {{@Transactional(rollbackFor = {java.io.IOException.class, java.sql.SQLException.class})}}
  // fix@qf2 {{Add rollbackFor = Exception.class}}
  // edit@qf2 [[sc=3;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  public void processOrder(Order order) throws IOException, SQLException {
  }

  @Transactional // Noncompliant [[quickfixes=qf3]]
//^^^^^^^^^^^^^^
  // fix@qf3 {{Add rollbackFor = Exception.class}}
  // edit@qf3 [[sc=3;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  public void importData() throws Exception {
  }

  @Transactional(timeout = 30) // Noncompliant
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  public void withOtherAttributes() throws SQLException {
  }

  @Transactional // Noncompliant [[quickfixes=qf7,qf8]]
//^^^^^^^^^^^^^^
  // fix@qf7 {{Add rollbackFor attribute}}
  // edit@qf7 [[sc=3;ec=17]] {{@Transactional(rollbackFor = checks.spring.CustomCheckedException.class)}}
  // fix@qf8 {{Add rollbackFor = Exception.class}}
  // edit@qf8 [[sc=3;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  public void customException() throws CustomCheckedException {
  }

  @Transactional // Noncompliant [[quickfixes=qf9,qf10]]
//^^^^^^^^^^^^^^
  // fix@qf9 {{Add rollbackFor attribute}}
  // edit@qf9 [[sc=3;ec=17]] {{@Transactional(rollbackFor = java.io.IOException.class)}}
  // fix@qf10 {{Add rollbackFor = Exception.class}}
  // edit@qf10 [[sc=3;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  public void mixedExceptions() throws IOException, RuntimeException {
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

  @Transactional // Noncompliant [[quickfixes=qf11,qf12]]
//^^^^^^^^^^^^^^
  // fix@qf11 {{Add rollbackFor attribute}}
  // edit@qf11 [[sc=3;ec=17]] {{@Transactional(rollbackFor = java.io.IOException.class)}}
  // fix@qf12 {{Add rollbackFor = Exception.class}}
  // edit@qf12 [[sc=3;ec=17]] {{@Transactional(rollbackFor = java.lang.Exception.class)}}
  static class ClassLevelNoConfig {
    public void noConfig() throws IOException {
    }

    @Transactional(rollbackFor = IOException.class)
    public void methodOverrides() throws IOException {
    }
  }

  @Transactional
  public void errorNotChecked() throws Error {
  }

  @org.springframework.transaction.annotation.Transactional // Noncompliant [[quickfixes=qf13,qf14]]
//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>^^^^^^^^^^^^^^^^^^^^^^^^
  // fix@qf13 {{Add rollbackFor attribute}}
  // edit@qf13 [[sc=3;ec=60]] {{@org.springframework.transaction.annotation.Transactional(rollbackFor = java.io.IOException.class)}}
  // fix@qf14 {{Add rollbackFor = Exception.class}}
  // edit@qf14 [[sc=3;ec=60]] {{@org.springframework.transaction.annotation.Transactional(rollbackFor = java.lang.Exception.class)}}
  public void fullyQualified() throws IOException {
  }

  @Transactional(rollbackFor = IOException.class)
  public void partialConfig() throws SQLException {
  }

  @Transactional(value = "txManager") // Noncompliant
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  public void withValueAttribute() throws IOException {
  }

  // Test nested structure to ensure parent traversal works
  static class OuterClass {
    @Transactional // Noncompliant
    static class InnerClassWithAnnotation {
      public void nestedMethod() throws IOException {
      }
    }

    static class InnerClassNoAnnotation {
      public void nestedMethodNoAnnotation() throws IOException {
        // No @Transactional at any level, so no issue
      }
    }
  }

  @Transactional // Noncompliant
  interface TransactionalInterface {
    void interfaceMethod() throws IOException;
  }

  // Test meta-annotated (composed) annotation
  @MyTransactional // Noncompliant
  public void metaAnnotated() throws IOException {
  }

  // Test unknown type (unresolved exception) - line 168 coverage
  @Transactional
  public void unknownException() throws UnresolvedCheckedException {
    // UnresolvedCheckedException is not imported/defined, so type.isUnknown() returns true
    // Should not raise an issue since we can't determine if it's a checked exception
  }

  // Test annotation with value attribute (transaction manager name)
  @Transactional("txManager") // Noncompliant
  public void valueShorthand() throws IOException {
    // Has value attribute but no rollback configuration
  }
}
