import java.sql.DriverManager;
import java.sql.SQLException;

class S2115 {
  void foo(Properties connectionProps) throws SQLException {
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", ""); // Noncompliant {{Add password protection to this database.}}
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", "Foo");

    String pwd = "";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd); // Noncompliant

    String pwd2 = "foo";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd2);

    String pRef = "";
    String pwd3 = pRef;

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd3); // Noncompliant

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true"); // Noncompliant

    DriverManager.getConnection("jdbc:db2://myhost:5021/mydb:user=dbadm;password=foo");
    DriverManager.getConnection("jdbc:db2://myhost:5021/mydb:user=dbadm;password="); // Noncompliant
    DriverManager.getConnection("jdbc:db2://myhost:5021/mydb:password=;user=dbadm"); // Noncompliant

    DriverManager.getConnection("jdbc:mysql://localhost:3306?user=dbadm&password=foo");
    DriverManager.getConnection("jdbc:mysql://localhost:3306?user=dbadm&password="); // Noncompliant
    DriverManager.getConnection("jdbc:mysql://localhost:3306?password=&user=dbadm"); // Noncompliant

    String string = "jdbc:db2://myhost:5021/mydb:user=dbadm;password=";
    DriverManager.getConnection(string); // Noncompliant

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", ConnectionProps);
  }
}
