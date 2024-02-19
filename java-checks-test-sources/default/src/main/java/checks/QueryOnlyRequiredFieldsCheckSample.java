package checks;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class QueryOnlyRequiredFieldsCheckSample {

  private static final String NON_COMPLIANT_SQL_QUERY = "SELECT * FROM table"; // Noncompliant {{Don't use the query "SELECT *".}}
  private static final String COMPLIANT_SQL_QUERY = "SELECT user FROM table"; // Compliant
  private String nonCompliantSqlQuery = "SELECT * FROM table"; // Noncompliant
  private String compliantSqlQuery = "SELECT user FROM table"; // Compliant

  public void examples(
    Connection connection,
    Statement statement,
    PreparedStatement preparedStatement,
    CallableStatement callableStatement,
    String sqlQuery,
    boolean b) throws SQLException {

    connection.prepareStatement(NON_COMPLIANT_SQL_QUERY);
    connection.prepareStatement(COMPLIANT_SQL_QUERY);

    try (PreparedStatement s = connection.prepareStatement(nonCompliantSqlQuery)) {
      s.execute();
    }

    try (PreparedStatement s = connection.prepareStatement(compliantSqlQuery)) {
      s.execute();
    }

    connection.prepareStatement("   selEcT * fRoM myTable"); // Noncompliant {{Don't use the query "SELECT *".}}
    connection.prepareStatement("   sElEcT user fRoM myTable"); // Compliant
    connection.prepareStatement("SELECTABLE 2*2 FROMAGE"); // Compliant
    connection.prepareCall("   sElEcT * fRoM myTable"); // Noncompliant

    String requestNonCompiliant = "   SeLeCt * FrOm myTable"; // Noncompliant
    connection.prepareStatement(requestNonCompiliant);

    String requestCompiliant = "   SeLeCt user FrOm myTable"; // Compliant
    connection.prepareStatement(requestCompiliant);

    connection.prepareCall("   sElEcT * fRoM myTable"); // Noncompliant

    statement.execute("SELECT     * FROM table"); // Noncompliant
    statement.execute("select * FROM table", 0); // Noncompliant
    statement.execute("SELECT *  FROM table", new int[0]); // Noncompliant
    statement.execute("SELeCT * FROM table", new String[0]); // Noncompliant
    statement.executeQuery("select * FROM table"); // Noncompliant

    String noSqlCompiliant = "SELECTABLE * FROM table"; // Compliant
    statement.execute(noSqlCompiliant);
    statement.execute("SELECT* FROM table"); // Compliant
    statement.execute("SELECT *FROM table"); // Compliant

    String requestNonCompiliant2 = requestNonCompiliant;
    preparedStatement.executeQuery(requestNonCompiliant2); // False Negative

    String undefinedQuery;
    if (b) {
      undefinedQuery = "Select * FrOm myTable"; // False Negative
    } else {
      undefinedQuery = "Select user FrOm myTable"; // Compliant
    }
    connection.prepareStatement(undefinedQuery);

    for (int i = 0; i < 10; i++) {
      connection.prepareStatement("SELECT * FROM table"); // Noncompliant
      connection.prepareStatement("SELECT user FROM table"); // Compliant
    }

    preparedStatement.executeQuery("SELECT * FROM table"); // Noncompliant
    callableStatement.execute("SELECT * FROM table"); // Noncompliant

    connection.prepareStatement("update * fRoM myTable"); // Compliant
    connection.prepareStatement(sqlQuery);
    statement.executeLargeUpdate("UPDATE * fRoM myTable"); // Compliant
    statement.executeUpdate("UPDATE * fRoM myTable"); // Compliant
  }
}
