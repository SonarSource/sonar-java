// place itself in the same package as the test to access the constants
package checks.security;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static checks.security.EmptyDatabasePasswordCheckSampleVariables.EMPTY_PASSWORD;
import static checks.security.EmptyDatabasePasswordCheckSampleVariables.NON_EMPTY_PASSWORD;

class EmptyDatabasePasswordCheckSample {
  void foo(Properties connectionProps, String unknown) throws SQLException {
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", ""); // Noncompliant [[sc=5;ec=86]] {{Add password protection to this database.}}
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", "Foo");

    String pwd = "";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd); // Noncompliant [[secondary=-1]]

    String pwd2 = "foo";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd2);

    String pRef = "";
    String pwd3 = pRef;

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd3); // Noncompliant [[secondary=-3,-2]]

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
    DriverManager.getConnection(string); // Noncompliant

    String url = null;
    DriverManager.getConnection(url);
    DriverManager.getConnection(null);


    String pwd4 = getPassword();
    pwd4 += "";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", pwd4);

    unknown += "";
    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", unknown);

    DriverManager.getConnection(
      "jdbc:mysql://sandy:secret@[myhost1:1111,address=(host=myhost2)(port=2222)(key2=value2)]/db");

    DriverManager.getConnection("jdbc:oracle:oci8:scott/tiger@myhost");

    DriverManager.getConnection("jdbc:oracle:oci8:   /@myhost"); // Compliant even though the URL is not accepted by the driver

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", "AppLogin", unknown);

    DriverManager.getConnection("jdbc:derby:memory:myDB;create=true", connectionProps);
  }

  String getPassword() {
    return "foo";
  }

  void missingPassword() throws SQLException {
    // empty (password=)
    DriverManager.getConnection("jdbc:mysql://address=(host=myhost1)(port=1111)(user=sandy)(password=),address=(host=myhost2)(port=2222)(user=finn)(password=)/db"); // Noncompliant
    // empty ,password=)
    DriverManager.getConnection("jdbc:mysql://[(host=myhost1,port=1111,user=sandy,password=secret),(host=myhost2,port=2222,user=finn,password=)]/db"); // Noncompliant

    // empty &password= at the end
    DriverManager.getConnection("jdbc:mysql://localhost:1111/db?user=user&password="); // Noncompliant
    // empty ?password=&
    DriverManager.getConnection("jdbc:mysql://localhost:1111/db?password=&user=user"); // Noncompliant



    // empty ;password=;
    DriverManager.getConnection("jdbc:derby:sample;password=;user=jill"); // Noncompliant
    // empty ;password= at the end
    DriverManager.getConnection("jdbc:derby:sample;user=jill;password="); // Noncompliant
  }

  void compliantNoPassword() throws SQLException {
    DriverManager.getConnection("jdbc:oracle:oci8:scott/@myhost");
    DriverManager.getConnection("jdbc:oracle:thin:scott/@//myhost:1521/myservicename");
    DriverManager.getConnection("jdbc:oracle:oci:scott/tiger/@");

    DriverManager.getConnection("jdbc:mysql://sandy@localhost:1111/db");
    DriverManager.getConnection("jdbc:mysql://sandy:@localhost:1111/db");
    DriverManager.getConnection("jdbc:mysql://sandy:@[myhost1:1111,address=(host=myhost2)(port=2222)(key2=value2)]/db");
    DriverManager.getConnection("jdbc:mysql://[(host=myhost1,port=1111,user=sandy),(host=myhost2,port=2222,user=finn)]/db");
    DriverManager.getConnection("jdbc:mysql://localhost:1111/db");

    DriverManager.getConnection("jdbc:derby:sample");

    DriverManager.getConnection("jdbc:db2://myhost:5021/mydb");
  }
}
