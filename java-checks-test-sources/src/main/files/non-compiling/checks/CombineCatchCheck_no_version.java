package checks;

import java.io.IOException;
import java.sql.SQLException;

class CombineCatchCheck {
  void foo() {
    try {
      canThrow();
    } catch (IOException e) {
      int i = 1;
    } catch (SQLException e) { // Noncompliant
      int i = 1;
    }

    try {
      canThrow();
    } catch (IOException e) {
      unknown(e);
    } catch (IllegalArgumentException e) { // Compliant, unknown methods
      unknown(e);
    } catch (SQLException e) { // Compliant, unknown methods
      unknown(e);
    }

    try {
      canThrow();
    } catch (IOException e) {
      unkown(e);
    } catch (IllegalArgumentException e) {
      handleException(e);
    }
  }

  void handleException(IllegalArgumentException io) { }

  void canThrow() throws IOException, SQLException, IllegalArgumentException {
  }
}
