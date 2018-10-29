import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Map.Entry;

abstract class A extends UnknownClassFromSamePackage {

  abstract PreparedStatement getPreparedStatement();
  abstract PreparedStatement getPreparedStatement(String s);
  abstract int getIntValue();
  abstract String getQuery();

  void foo(Connection connection) throws SQLException {
    PreparedStatement ps = connection.prepareStatement("SELECT fname, lname FROM employees where hireDate > ? and salary < ?");

    ps.setDate(0, new Date(0)); // Noncompliant [[sc=16;ec=17]] {{PreparedStatement indices start at 1.}}
    ps.setDouble(3, 0.0); // Noncompliant [[sc=18;ec=19]] {{This "PreparedStatement" only has 2 parameters.}}
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
    ps.setDouble(3, 0.0); // Noncompliant [[sc=18;ec=19]] {{This "PreparedStatement" has no parameters.}}
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

  void hio(boolean test) throws SQLException {
    PreparedStatement ps = getPreparedStatement("SELECT fname, lname FROM employees where hireDate > 1986");

    if (test) {
      ps = getPreparedStatement("SELECT fname, lname FROM employees where hireDate > ? and salary < ?");
      ps.setDouble(1, 0.0); // Compliant - last assignment is used
      ps.setDouble(2, 0.0); // Compliant
    }

    ps = getPreparedStatement("SELECT fname, lname FROM employees where hireDate > 1986");

    PreparedStatement ps2 = getPreparedStatement("SELECT fname, lname FROM employees where hireDate > ? and salary < ?");
    ps2.setDouble(1, 0.0); // Compliant

    int a;
    a = 2;

    int[] b = new int[1];
    b[0] = 3;
  }

  void unknownQuery() throws SQLException {
    PreparedStatement ps = getPreparedStatement(UNKNOWN_QUERY_FROM_UNKNOWN_PARENT); // Compliant
    ps.setDouble(2, 0.0);
  }

  void false_negative(boolean test) throws SQLException {
    PreparedStatement ps;

    if (test) {
      ps = getPreparedStatement("SELECT fname, lname FROM employees where hireDate > ?");
    } else {
      ps = getPreparedStatement("SELECT fname, lname FROM employees where hireDate > ? and salary < ?");
    }

    ps.setDouble(1, 0.0); // Compliant - last assignment is used
    ps.setDouble(2, 0.0); // Compliant FALSE NEGATIVE - in then would have been applied, there would be no 2nd parameter (CFG?)
  }

  public void reproducer(Connection connection) throws SQLException {
    String selectClause = ";";
    selectClause = "SELECT anything FROM somewhere "
      + selectClause;

    PreparedStatement statement = connection.prepareStatement(selectClause);
    statement.setString(1, "anything"); // Noncompliant {{This "PreparedStatement" has no parameters.}}
  }

  public void updateCoffeeSales(HashMap<String, Integer> salesForWeek, Connection con, String param) throws SQLException {

    String dbName = "doug";

    PreparedStatement updateSales = null;
    PreparedStatement updateTotal = null;
    PreparedStatement other = null;

    String updateString = "update " + dbName + ".COFFEES set SALES = ?";

    String updateStatement =
          "update " + dbName + ".COFFEES " +
                  "set TOTAL = TOTAL + ? " +
                  "where COF_NAME = ?";

    try {
      PreparedStatement ps = con.prepareStatement(updateStatement);
      ps.setInt(1, 1); // Compliant
      ps.setString(3, "three"); // Noncompliant
      ps.setString(72, "boom"); // Noncompliant

      ps = con.prepareStatement(updateStatement);
      ps.setInt(1, 2); // Compliant
      ps.setString(2, "three"); // Compliant

      updateSales = con.prepareStatement(updateString);
      updateTotal = con.prepareStatement(updateStatement);
      other = con.prepareStatement("update " + dbName + ".COFFEES set SALES = ?");

      for (Map.Entry<String, Integer> e : salesForWeek.entrySet()) {
        updateSales.setInt(1, e.getValue().intValue()); // Compliant
        updateSales.setString(2, e.getKey());  // Noncompliant
        updateTotal.setInt(1, e.getValue().intValue()); // Compliant
        updateTotal.setString(2, e.getKey()); // Compliant
        other.setInt(2, getIntValue()); // Noncompliant
      }

      updateString = "update " + param + ".COFFEES set SALES = ?";

      PreparedStatement testParam = con.prepareStatement(updateString);
      testParam.setInt(3, 0); // Noncompliant

      testParam = con.prepareStatement(param + " update");
      testParam.setInt(3, 0); // Noncompliant

      testParam = con.prepareStatement(param + param);
      testParam.setInt(3, 0); // Compliant

      String[] array = new String[]{""};
      PreparedStatement qix = con.prepareStatement(array[0]);
      qix.setString(3, ""); // Compliant
    } catch(SQLException e) {
    }
  }

  public class Example {

    private final String REQUETE_SELECT_RESA_RESEAU = "SELECT COLUMN1 FROM TABLE";

    private final String CLAUSE_ETAT = " WHERE COLUMN2 = ?";

    public Example() {
    }

    public synchronized void method1() {

      String req= REQUETE_SELECT_RESA_RESEAU;
      req = req + CLAUSE_ETAT; //StackOverflowError
      PreparedStatement pstmt= m_con.prepareStatement(req);
      pstmt.setInt(1,10);
      ResultSet rs=pstmt.executeQuery();

    }

  }

  private class IndirectInititalization {
    Connection conn;

    void foo() {
      PreparedStatement ps = null;
      try {
        ps = prepareInsertRequest("SomeTable", 2);
        ps.setString(1, "someValue");
        ps.setInt(2, 4);
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    public PreparedStatement prepareInsertRequest(String tableName, int argsNum) throws SQLException {
      return conn.prepareStatement("INSERT INTO " + tableName + " VALUES (?" + Strings.repeat(",?", argsNum - 1) + ")");
    }
  }

  void foo(Connection conn) throws SQLException {
    String q = "SELECT * FROM table WHERE name=?";
    q += " AND c IS NOT NULL";
    PreparedStatement ps = conn.prepareStatement(q);
    ps.setString(1, "someValue"); // compliant, += with constant.
  }
}
