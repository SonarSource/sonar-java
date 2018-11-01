import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.SQLException;

class A {

  public foo() {
    Logger logger = java.util.logging.Logger.getAnonymousLogger("");
    try {

    } catch (SQLException e) { // Noncompliant [[secondary=12, 13]] {{Either log this exception and handle it, or rethrow it with some contextual information.}}
      logger.log(Level.ALL, "", e);
      throw new MySQLException(contextInfo, e);
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
      foo;
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
