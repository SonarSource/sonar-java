package checks;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

class LoggedRethrownExceptionsCheckSample {
  static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(LoggedRethrownExceptionsCheckSample.class);
  Object contextInfo;
  boolean flag;
  int foo;

  public void foo() throws Exception {
    Logger logger = java.util.logging.Logger.getAnonymousLogger("");
    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant {{Either log this exception and handle it, or rethrow it with some contextual information.}}
      logger.log(Level.ALL, "", e);
//  ^^^<
      throw new MySQLException(contextInfo, e);
//  ^^^<
    }

    try {
      doSomething();
    } catch (SQLException e) { // Compliant
      logger.log(Level.ALL, "", e);
      throw new MySQLException(contextInfo);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      logger.log(Level.ALL, "MyError: " + e);
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      logger.log(Level.ALL, e.getMessage());
      throw new MySQLException(contextInfo, e);
    }


    try {
      doSomething();
    } catch (SQLException e) { // Compliant
      e.getMessage();
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      logger.logp(Level.ALL, "foo", "bar", e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      logger.logrb(Level.ALL, "foo", "bar", "", e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      logger.config(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      logger.info(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      logger.throwing("foo", "bar", e);
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      logger.severe(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      logger.warning(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      slf4jLogger.debug(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      slf4jLogger.error(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      slf4jLogger.info(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      slf4jLogger.trace(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Noncompliant
      slf4jLogger.warn(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Compliant
      logger.log(Level.ALL, "MyError: " + e);
      handleException(e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Compliant
      logger.log(Level.ALL, "", logger);
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Compliant
      anotherMethod(Level.ALL, "", e);
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Compliant
      if (flag) {
        logger.log(Level.ALL, "", e);
      }
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Compliant
      foo++;
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Compliant
      throw new MySQLException(contextInfo, e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Compliant
      logger.log(Level.ALL, "", e);
    }

    try {
      doSomething();
    } catch (SQLException e) { // Compliant
    }

    Exception e1 = new Exception();
    logger.log(Level.ALL, "", logger); // Compliant, not inside catch
    throw new MySQLException(contextInfo, e1);
  }

  private static void doSomething() throws SQLException { }
  private void anotherMethod(Level all, String string, SQLException e) { }
  private void handleException(Exception e) { }

  private static class MySQLException extends SQLException {
    public MySQLException(Object o, Exception e) { }
    public MySQLException(Object o) { }
  }
}
