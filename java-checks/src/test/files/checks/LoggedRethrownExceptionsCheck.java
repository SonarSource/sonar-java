import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.SQLException;
import org.slf4j.LoggerFactory;

class A {

  public void foo() {
    Logger logger = java.util.logging.Logger.getAnonymousLogger("");
    final static org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(A.class);
    try {

    } catch (SQLException e) { // Noncompliant [[secondary=14, 15]] {{Either log this exception and handle it, or rethrow it with some contextual information.}}
      logger.log(Level.ALL, "", e);
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Compliant
      logger.log(Level.ALL, "", e);
      throw new MySQLException(contextInfo);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      logger.log(Level.ALL, "MyError: " + e);
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      logger.log(Level.ALL, e.getMessage());
      throw new MySQLException(contextInfo, e);
    }


    try {

    } catch (SQLException e) { // Compliant
      e.getMessage();
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      logger.logp(Level.ALL, "foo", "bar", e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      logger.logrb(Level.ALL, "foo", "bar", "", e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      logger.config(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      logger.info(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      logger.throwing("foo", "bar", e);
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      logger.severe(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      logger.warning(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      slf4jLogger.debug(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      slf4jLogger.error(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      slf4jLogger.info(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      slf4jLogger.trace(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Noncompliant
      slf4jLogger.warn(e.getMessage());
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Compliant
      logger.log(Level.ALL, "MyError: " + e);
      handleException(e);
    }

    try {

    } catch (SQLException e) { // Compliant
      logger.log(Level.ALL, "", logger);
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Compliant
      anotherMethod(Level.ALL, "", e);
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Compliant
      throw new MySQLException(contextInfo, e);
      if (flag) {
        logger.log(Level.ALL, "", e);
      }
    }

    try {

    } catch (SQLException e) { // Compliant
      throw new MySQLException(contextInfo, e);
      foo++;
    }

    try {

    } catch (SQLException e) { // Compliant
      throw new MySQLException(contextInfo, e);
    }

    try {

    } catch (SQLException e) { // Compliant
      logger.log(Level.ALL, "", e);
    }

    try {

    } catch (SQLException e) { // Compliant
    }

    logger.log(Level.ALL, "", logger); // Compliant, not inside catch
    throw new MySQLException(contextInfo, e);
  }
}
