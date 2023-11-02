package checks;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ResultSetTypeForwardUnsafeMethodsCheckSample {

  void nonCompliant(Connection con){
    try (Statement stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
      stmt.executeQuery("SELECT name, address FROM PERSON");
      ResultSet rs = stmt.getResultSet();
      if (rs.isBeforeFirst()) { // Noncompliant {{Remove this call to "isBeforeFirst".}}
        // Do something
      }
    }catch (SQLException e) {
      e.printStackTrace();
    }
  }

  void compliant(Connection con){
    try (Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
      stmt.executeQuery("SELECT name, address FROM PERSON");
      ResultSet rs = stmt.getResultSet();
      if (rs.isBeforeFirst()) { // Compliant
        // Do something
      }
    }catch (SQLException e) {
      e.printStackTrace();
    }
  }

}
