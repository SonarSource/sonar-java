package checks;

import java.sql.SQLException;
import java.sql.Statement;

class SQLInjectionSample {
  private final Statement stmt;

  public SQLInjectionSample(Statement stmt) {
    this.stmt = stmt;
  }

  public void formatInline(String input) throws SQLException {
    this.stmt.execute(String.format("SELECT %s", input)); // Noncompliant
  }

  public void formatLocale(Locale locale, int input, Unknown unknown) throws SQLException {
    // Do not generate warnings on unknown types to avoid FPs.
    String query = String.format(locale, "SELECT %s %s", input, unknown);
    this.stmt.execute(query);
  }
}
