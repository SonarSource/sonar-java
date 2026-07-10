package checks.spring;

import java.io.IOException;
import java.sql.SQLException;
import org.springframework.transaction.annotation.Transactional;

class Order {}
class NotificationException extends Exception {}
class CustomCheckedException extends Exception {}

public class TransactionalMethodCheckedExceptionCheckSample {

  @Transactional // Noncompliant {{Specify rollback behavior for checked exceptions using "rollbackFor" or "noRollbackFor" attributes.}} [[quickfixes=qf1,qf2]]
//^^^^^^^^^^^^^^
  // fix@qf1 {{Add rollbackFor attribute}}
  // edit@qf1 [[sc=3;ec=17]] {{@Transactional(rollbackFor = {IOException.class, SQLException.class})}}
  // fix@qf2 {{Add rollbackFor = Exception.class}}
  // edit@qf2 [[sc=3;ec=17]] {{@Transactional(rollbackFor = Exception.class)}}
  public void processOrder(Order order) throws IOException, SQLException {
  }

  @Transactional // Noncompliant [[quickfixes=qf3,qf4]]
//^^^^^^^^^^^^^^
  // fix@qf3 {{Add rollbackFor attribute}}
  // edit@qf3 [[sc=3;ec=17]] {{@Transactional(rollbackFor = Exception.class)}}
  // fix@qf4 {{Add rollbackFor = Exception.class}}
  // edit@qf4 [[sc=3;ec=17]] {{@Transactional(rollbackFor = Exception.class)}}
  public void importData() throws Exception {
  }

  @Transactional(timeout = 30) // Noncompliant
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  public void withOtherAttributes() throws SQLException {
  }

  @Transactional // Noncompliant [[quickfixes=qf7,qf8]]
//^^^^^^^^^^^^^^
  // fix@qf7 {{Add rollbackFor attribute}}
  // edit@qf7 [[sc=3;ec=17]] {{@Transactional(rollbackFor = CustomCheckedException.class)}}
  // fix@qf8 {{Add rollbackFor = Exception.class}}
  // edit@qf8 [[sc=3;ec=17]] {{@Transactional(rollbackFor = Exception.class)}}
  public void customException() throws CustomCheckedException {
  }

  @Transactional // Noncompliant [[quickfixes=qf9,qf10]]
//^^^^^^^^^^^^^^
  // fix@qf9 {{Add rollbackFor attribute}}
  // edit@qf9 [[sc=3;ec=17]] {{@Transactional(rollbackFor = IOException.class)}}
  // fix@qf10 {{Add rollbackFor = Exception.class}}
  // edit@qf10 [[sc=3;ec=17]] {{@Transactional(rollbackFor = Exception.class)}}
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
  // edit@qf11 [[sc=3;ec=17]] {{@Transactional(rollbackFor = IOException.class)}}
  // fix@qf12 {{Add rollbackFor = Exception.class}}
  // edit@qf12 [[sc=3;ec=17]] {{@Transactional(rollbackFor = Exception.class)}}
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
  // edit@qf13 [[sc=3;ec=60]] {{@org.springframework.transaction.annotation.Transactional(rollbackFor = IOException.class)}}
  // fix@qf14 {{Add rollbackFor = Exception.class}}
  // edit@qf14 [[sc=3;ec=60]] {{@org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)}}
  public void fullyQualified() throws IOException {
  }

  @Transactional(rollbackFor = IOException.class)
  public void partialConfig() throws SQLException {
  }
}
