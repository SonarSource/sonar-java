import java.sql.DriverManager;
import java.sql.SQLException;
import static org.sonar.java.checks.security.EmptyDatabasePasswordCheckTest.EMPTY_PASSWORD;
import static org.sonar.java.checks.security.EmptyDatabasePasswordCheckTest.NON_EMPTY_PASSWORD;

class S2115 {
  void foo(Properties connectionProps, String unknown) throws SQLException {
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", ""); // Noncompliant [[sc=5;ec=86]] {{Add password protection to this database.}}
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", "Foo");

    String pwd = "";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd); // Noncompliant

    String pwd2 = "foo";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd2);

    String pRef = "";
    String pwd3 = pRef;

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd3); // Noncompliant

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", getPassword());

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", EMPTY_PASSWORD); // Noncompliant [[sc=5;ec=98]]
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", NON_EMPTY_PASSWORD);

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", unknown);

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", null);

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

    String url = null;
    DriverManager.getConnection(url);
    DriverManager.getConnection(null);

  }

  String getPassword() {
    return "foo";
  }
}
