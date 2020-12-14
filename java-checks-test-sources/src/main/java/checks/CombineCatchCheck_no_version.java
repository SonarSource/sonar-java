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

    try {
      canThrow();
    } catch (IOException e) {
      handleException(e);
    } catch (IllegalArgumentException e) { // Compliant, specific type is need in the body
      handleException(e);
    } catch (SQLException e) { // Compliant, not the same body
      handleException(e, "a");
    } catch (Exception e) { // Compliant
      int i;
    } catch (Throwable e) { // Compliant
      doCleanup();
    }

    try {
      canThrow();
    } catch (IOException e) {
      for (int i = 0; i < 1; i++) {};
    } catch (Exception e) { // Update part is not the same.
      for (int i = 0; i < 1; foo()) {};
    }
  }

  void canThrow() throws IOException, SQLException, IllegalArgumentException {
  }

  private void doCleanup() {
  }

  void handleException(IOException io) { }

  void handleException(IllegalArgumentException io) { }

  void handleException(SQLException io, String s) { }
}

class CombineCatchCheckLogger {
  void log(Exception e) {
  }
}
