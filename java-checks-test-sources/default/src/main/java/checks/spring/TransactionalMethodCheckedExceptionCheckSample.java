package checks.spring;

import java.io.IOException;
import java.sql.SQLException;
import org.springframework.transaction.annotation.Transactional;

class Order {}
class NotificationException extends Exception {}
class CustomCheckedException extends Exception {}

public class TransactionalMethodCheckedExceptionCheckSample {

  @Transactional
  public void processOrder(Order order) throws IOException, SQLException { // Noncompliant {{Specify rollback behavior for checked exceptions using "rollbackFor" or "noRollbackFor" attributes.}}
//            ^^^^^^^^^^^^
  }

  @Transactional
  public void importData() throws Exception { // Noncompliant
//            ^^^^^^^^^^
  }

  @Transactional(timeout = 30)
  public void withOtherAttributes() throws SQLException { // Noncompliant
//            ^^^^^^^^^^^^^^^^^^^
  }

  @Transactional
  public void customException() throws CustomCheckedException { // Noncompliant
//            ^^^^^^^^^^^^^^^
  }

  @Transactional
  public void mixedExceptions() throws IOException, RuntimeException { // Noncompliant
//            ^^^^^^^^^^^^^^^
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
    public void noConfig() throws IOException { // Noncompliant
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
  public void fullyQualified() throws IOException { // Noncompliant
//            ^^^^^^^^^^^^^^
  }

  @Transactional(rollbackFor = IOException.class)
  public void partialConfig() throws SQLException {
  }
}
