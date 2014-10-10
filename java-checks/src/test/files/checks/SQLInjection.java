import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import org.hibernate.Session;
class A {
  private static final String CONSTANT = "SELECT * FROM TABLE";
  public void method(String param, String param2) {
    try {
      Connection conn = DriverManager.getConnection("url", "user1", "password");
      Statement stmt = conn.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT Lname FROM Customers WHERE Snum = 2001");//Compliant
      rs = stmt.executeQuery("SELECT Lname FROM Customers WHERE Snum = "+param); //NonCompliant
      String query = "SELECT Lname FROM Customers WHERE Snum = "+param;
      rs = stmt.executeQuery(query);//NonCompliant

      boolean bool = false;
      String query2 = "Select Lname ";
      if(bool) {
        query2 += "FROM Customers";
      }else {
        query2 += "FROM Providers";
      }
      query2 = query2 + " WHERE Snum =2001";
      rs = stmt.executeQuery(query2); //Compliant

      //Prepared statement
      PreparedStatement ps = conn.prepareStatement("SELECT Lname FROM Customers"+" WHERE Snum = 2001"); //OK
      ps.executeQuery(query); //NonCompliant
      ps  = conn.prepareStatement("SELECT Lname FROM Customers WHERE Snum = "+param); //NonCompliant
      ps = conn.prepareStatement(query); //NonCompliant
      ps = conn.prepareStatement(query2); //Compliant

      //Callable Statement
      CallableStatement cs = conn.prepareCall("SELECT Lname FROM Customers WHERE Snum = 2001"); //OK
      cs.executeQuery(query); //NonCompliant
      cs  = conn.prepareCall("SELECT Lname FROM Customers WHERE Snum = "+param2); //NonCompliant
      cs = conn.prepareCall(query); //NonCompliant
      cs = conn.prepareCall(query2); //Compliant
      cs = conn.prepareCall(CONSTANT); //Compliant
      cs = conn.prepareCall(foo()); //Compliant this is not a parameter
      String query3 = "SELECT * from table";
      cs = conn.prepareCall(query3); //Compliant

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
      session.createQuery("From Customer where id > "+param); //NonCompliant
      session.createQuery(query); //NonCompliant
      conn.prepareStatement(param);
      conn.prepareStatement(sqlQuery + "plop"); //compliant sqlQuery is a field
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
}