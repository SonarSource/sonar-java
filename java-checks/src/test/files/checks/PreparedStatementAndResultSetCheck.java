import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.GregorianCalendar;

class A {
  void foo(Connection connection) throws SQLException {
    PreparedStatement ps = connection.prepareStatement("SELECT fname, lname FROM employees where hireDate > ? and salary < ?");

    ps.setDate(0, new Date(0)); // Noncompliant {{PreparedStatement indices start at 1.}}
    ps.setDouble(3, 0.0); // Noncompliant {{This "PreparedStatement" only has 2 parameters.}}
    ps.setString(getIntValue(), ""); // Compliant - first argument can not be evaluated
    ps.setInt(1, 0); // Compliant

    ResultSet rs = ps.executeQuery();
    rs.getString(0); // Noncompliant {{ResultSet indices start at 1.}}
    rs.getDate(0, new GregorianCalendar()); // Noncompliant {{ResultSet indices start at 1.}}
    rs.getString(1); // Compliant
  }
  
  void bar(Connection connection) throws SQLException {
    PreparedStatement ps = connection.prepareStatement("SELECT fname, lname FROM employees where hireDate > 1986");

    ps.setDate(0, new Date(0)); // Noncompliant {{PreparedStatement indices start at 1.}}
    ps.setDouble(3, 0.0); // Noncompliant {{This "PreparedStatement" has no parameters.}}
  }
  
  void dam(Connection connection, String query) throws SQLException {
    PreparedStatement ps = connection.prepareStatement(query);

    ps.setDate(0, new Date(0)); // Noncompliant {{PreparedStatement indices start at 1.}}
    ps.setDouble(3, 0.0); // Compliant - Query of the preparedStatement is unknown
  }
  
  void cro(PreparedStatement ps) throws SQLException {
    ps.setDate(0, new Date(0)); // Noncompliant {{PreparedStatement indices start at 1.}}
    ps.setDouble(3, 0.0); // Compliant - Query of the preparedStatement is unknown
  }
  
  void elk() throws SQLException {
    getPreparedStatement().setDate(0, new Date(0)); // Noncompliant {{PreparedStatement indices start at 1.}}
    getPreparedStatement().setDouble(3, 0.0); // Compliant - Query of the preparedStatement is unknown
  }
  
  void gra() throws SQLException {
    PreparedStatement ps = getPreparedStatement();
    
    ps.setDate(0, new Date(0)); // Noncompliant {{PreparedStatement indices start at 1.}}
    ps.setDouble(3, 0.0); // Compliant - Query of the preparedStatement is unknown
    
    PreparedStatement ps2 = ps;
    ps2.setDate(0, new Date(0)); // Noncompliant {{PreparedStatement indices start at 1.}}
    ps2.setDouble(3, 0.0); // Compliant - Query of the preparedStatement is unknown
  }

  int getIntValue() {
    return 0;
  }
  
  PreparedStatement getPreparedStatement() {
    return null;
  }
  
}