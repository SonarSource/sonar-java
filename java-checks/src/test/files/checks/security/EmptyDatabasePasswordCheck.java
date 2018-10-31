import java.sql.DriverManager;
import java.sql.SQLException;
import static org.sonar.java.checks.security.EmptyDatabasePasswordCheckTest.EMPTY_PASSWORD;
import static org.sonar.java.checks.security.EmptyDatabasePasswordCheckTest.NON_EMPTY_PASSWORD;

class S2115 {
  void foo(Properties connectionProps, String unknown) throws SQLException {
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", ""); // Noncompliant [[sc=5;ec=86]] {{Add password protection to this database.}}
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", "Foo");

    String pwd = "";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd); // FN

    String pwd2 = "foo";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd2);

    String pRef = "";
    String pwd3 = pRef;

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd3); // FN

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", getPassword());

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", EMPTY_PASSWORD); // Noncompliant [[sc=5;ec=98]]
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", NON_EMPTY_PASSWORD);

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", unknown);

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", null);

    DriverManager.getConnection("jdbc:db2://myhost:5021/mydb:user=dbadm;password=foo");
    DriverManager.getConnection("jdbc:db2://myhost:5021/mydb:user=dbadm;password="); // Noncompliant
    DriverManager.getConnection("jdbc:db2://myhost:5021/mydb:password=;user=dbadm"); // Noncompliant

    DriverManager.getConnection("jdbc:mysql://localhost:3306?user=dbadm&password=foo");
    DriverManager.getConnection("jdbc:mysql://localhost:3306?user=dbadm&password="); // Noncompliant
    DriverManager.getConnection("jdbc:mysql://localhost:3306?password=&user=dbadm"); // Noncompliant

    String string = "jdbc:db2://myhost:5021/mydb:user=dbadm;password=";
    DriverManager.getConnection(string); // FN

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", ConnectionProps);

    String url = null;
    DriverManager.getConnection(url);
    DriverManager.getConnection(null);

    String url2 = javax.sql.DataSource.getConnection().getMetadata().getURL();
    url2 += (";shutdown=true");
    DriverManager.getConnection(url2);

    String pwd4 = getPassword();
    pwd4 += "";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd4);

    unknown += "";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", unknown);

    DriverManager.getConnection(
      "jdbc:mysql://sandy:secret@[myhost1:1111,address=(host=myhost2)(port=2222)(key2=value2)]/db");

    DriverManager.getConnection("jdbc:oracle:oci8:scott/tiger@myhost");

    DriverManager.getConnection("jdbc:mysql://sandy:@[myhost1:1111,address=(host=myhost2)(port=2222)(key2=value2)]/db"); // Noncompliant

    DriverManager.getConnection("jdbc:oracle:oci8:scott/@myhost"); // Noncompliant

    DriverManager.getConnection("jdbc:oracle:oci8:   /@myhost"); // Noncompliant
  }

  String getPassword() {
    return "foo";
  }
}
