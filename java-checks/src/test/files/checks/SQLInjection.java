import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import org.hibernate.Session;
import javax.persistence.EntityManager;
import javax.persistence.Query;

class A {
  private static final String CONSTANT = "SELECT * FROM TABLE";
  public void method(String param, String param2, EntityManager entityManager) {
    try {
      Connection conn = DriverManager.getConnection("url", "user1", "password");
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT Lname FROM Customers WHERE Snum = 2001");
      rs = stmt.executeQuery("SELECT Lname FROM Customers WHERE Snum = "+param); // Noncompliant [[sc=17;ec=29]] {{"param" is provided externally to the method and not sanitized before use.}}
      String query = "SELECT Lname FROM Customers WHERE Snum = "+param;
      rs = stmt.executeQuery(query); // Noncompliant

      boolean bool = false;
      String query2 = "Select Lname ";
      if(bool) {
        query2 += "FROM Customers";
      }else {
        query2 += "FROM Providers";
      }
      query2 = query2 + " WHERE Snum =2001";
      rs = stmt.executeQuery(query2);

      //Prepared statement
      PreparedStatement ps = conn.prepareStatement("SELECT Lname FROM Customers"+" WHERE Snum = 2001");
      ps.executeQuery(query); // Noncompliant
      ps  = conn.prepareStatement("SELECT Lname FROM Customers WHERE Snum = "+param); // Noncompliant
      ps = conn.prepareStatement(query); // Noncompliant
      ps = conn.prepareStatement(query2);

      //Callable Statement
      CallableStatement cs = conn.prepareCall("SELECT Lname FROM Customers WHERE Snum = 2001");
      cs.executeQuery(query); // Noncompliant
      cs  = conn.prepareCall("SELECT Lname FROM Customers WHERE Snum = "+param2); // Noncompliant {{"param2" is provided externally to the method and not sanitized before use.}}
      cs = conn.prepareCall(query); // Noncompliant
      cs = conn.prepareCall(query2);
      cs = conn.prepareCall(CONSTANT);
      cs = conn.prepareCall(foo());
      String query3 = "SELECT * from table";
      cs = conn.prepareCall(query3);

      String s;
      String tableName = "TableName";
      String column = " column ";
      String FROM = " FROM ";
      if(true) {
        s = "SELECT" +column+FROM +tableName;
      } else {
        s = "SELECT" +column+"FROM" +tableName;
      }
      cs = conn.prepareCall(s);
      String request = foo() + " FROM table";
      cs = conn.prepareCall(request);
      new A().prepareStatement(query);
      A a = new A();
      a.prepareStatement(query);
      ps.executeQuery();


      Session session;
      session.createQuery("From Customer where id > ?");
      session.createQuery("From Customer where id > "+param); // Noncompliant {{Use Hibernate's parameter binding instead of concatenation.}}
      session.createQuery(query); // Noncompliant {{Use Hibernate's parameter binding instead of concatenation.}}
      conn.prepareStatement(param);
      conn.prepareStatement(sqlQuery + "plop");

      String sql = "SELECT lastname, firstname FROM employee where uid = '" + param + "'";
      entityManager.createNativeQuery(sql); // Noncompliant {{"param" is provided externally to the method and not sanitized before use.}}
    } catch (Exception e) {
    }
  }

  String foo() {
    return "SELECT * ";
  }

  private String sqlQuery;
  class A {
    void prepareStatement(String s) {

    }
  }

  private static void makeQuery(Connection p_con) {
    try {
      String query = null;
      StringBuffer qryBuffer = new StringBuffer();

      qryBuffer = new StringBuffer();
      qryBuffer.append(" select abc from xyz ");
      qryBuffer.append(" where bulubulu=?");
      query = qryBuffer.toString();

      p_con.prepareStatement(query); // Compliant
    } catch (Exception e) {
      System.out.println("makeQuery");
    }
  }
}
