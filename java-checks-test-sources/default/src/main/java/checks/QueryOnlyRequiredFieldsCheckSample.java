package checks;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class QueryOnlyRequiredFieldsCheckSample {

  public void noncompliantExamples(
    Connection connection,
    Statement statement,
    PreparedStatement preparedStatement,
    CallableStatement callableStatement,
    String sqlQuery) throws SQLException {

    connection.prepareStatement("   selEcT * fRoM myTable"); // Noncompliant {{Don't use the query "SELECT *".}}
    connection.prepareStatement("   sElEcT user fRoM myTable");
    connection.prepareStatement("SELECTABLE 2*2 FROMAGE");
    connection.prepareCall("   sElEcT * fRoM myTable"); // Noncompliant

    String requestNonCompiliant = "   SeLeCt * FrOm myTable"; // Noncompliant
    connection.prepareStatement(requestNonCompiliant);

    String requestCompiliant = "   SeLeCt user FrOm myTable";
    connection.prepareStatement(requestCompiliant);

    connection.prepareCall("   sElEcT * fRoM myTable"); // Noncompliant

    statement.execute("SELECT     * FROM table"); // Noncompliant
    statement.execute("select * FROM table", 0); // Noncompliant
    statement.execute("SELECT *  FROM table", new int[0]); // Noncompliant
    statement.execute("SELeCT * FROM table", new String[0]); // Noncompliant
    statement.executeQuery("select * FROM table"); // Noncompliant

    String noSqlCompiliant = "SELECTABLE * FROM table";
    statement.execute(noSqlCompiliant);
    statement.execute("SELECT* FROM table");
    statement.execute("SELECT *FROM table");

    String requestNonCompiliant2 = requestNonCompiliant;
    preparedStatement.executeQuery(requestNonCompiliant2); // False Negative

    preparedStatement.executeQuery("SELECT * FROM table"); // Noncompliant
    callableStatement.execute("SELECT * FROM table"); // Noncompliant

    connection.prepareStatement("update * fRoM myTable");
    connection.prepareStatement(sqlQuery);
    statement.executeLargeUpdate("UPDATE * fRoM myTable");
    statement.executeUpdate("UPDATE * fRoM myTable");
  }
}
