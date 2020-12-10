package checks;

import java.io.IOException;
import java.sql.SQLException;

class CombineCatchCheck {
  void foo() {
    CombineCatchCheckLogger logger = new CombineCatchCheckLogger();

    try {
      canThrow();
    } catch (IOException e) {
      doCleanup();
      logger.log(e);
    }
    catch (SQLException e) {  // Noncompliant {{Combine this catch with the one at line 12, which has the same body. (sonar.java.source not set. Assuming 7 or greater.)}}
      doCleanup();
      logger.log(e);
    }
    catch (IllegalArgumentException  e) {  // Compliant; block contents are different
      doCleanup();
      throw e;
    }

    try {
      canThrow();
    } catch (IOException | IllegalArgumentException e) {
      doCleanup();
      logger.log(e);
    }
    catch (SQLException e) {  // Noncompliant {{Combine this catch with the one at line 27, which has the same body. (sonar.java.source not set. Assuming 7 or greater.)}}
      doCleanup();
      logger.log(e);
    }
    catch (ArrayStoreException  e) {  // Compliant; block contents are different
      doCleanup();
      throw e;
    }
  }

  void canThrow() throws IOException, SQLException, IllegalArgumentException {
  }

  private void doCleanup() {
  }
}

class CombineCatchCheckLogger {
  void log(Exception e) {
  }
}
