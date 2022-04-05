package checks;

import java.net.PasswordAuthentication;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.naming.Context;

class HardCodedPasswordCheck {

  String fieldNameWithPasswordInIt = retrievePassword();

  private static final String PASSED = "passed"; // compliant nothing to do with passwords
  private static final String EMPTY = "";

  private void a(char[] pwd, String var) throws SQLException {
    String variable1 = "blabla";
    String variable2 = "login=a&password=xxx"; // Noncompliant [[sc=24;ec=46]] {{'password' detected in this expression, review this potentially hard-coded password.}}
    String variable3 = "login=a&passwd=xxx"; // Noncompliant
    String variable4 = "login=a&pwd=xxx"; // Noncompliant
    String variable5 = "login=a&password=";
    String variable6 = "login=a&password= ";

    String query1 = "password=?"; // Compliant
    String query1_1 = "password=???"; // Compliant
    String query1_2 = "password=X"; // Compliant
    String query1_3 = "password=anonymous"; // Compliant
    String query4 = "password='" + pwd + "'"; // Compliant
    String query2 = "password=:password"; // Compliant
    String query3 = "password=:param"; // Compliant
    String query5 = "password=%s"; // Compliant
    String query6 = "password=\"%s\""; // Compliant
    String query7 = "\"password=\""; // Compliant

    // Constant used to avoid duplicated string
    String passwordConst = "Password"; // Compliant
    String pwds = "pwd"; // Compliant
    final String PASSWORD = "Password"; // Compliant
    final String PASSWORD_INPUT = "[id='password']"; // Compliant
    final String PASSWORD_PROPERTY = "custom.password"; // Compliant
    final String TRUSTSTORE_PASSWORD = "trustStorePassword"; // Compliant
    final String CONNECTION_PASSWORD = "connection.password"; // Compliant
    final String RESET_PASSWORD = "/users/resetUserPassword"; // Compliant
    final String RESET_PWD = "/users/resetUserPassword"; // Compliant
    char[] passwordToChar = "password".toCharArray(); // Compliant
    char[] passwordToChar2 = "http-password".toCharArray(); // Compliant
    String passwordToString = "http-password".toString(); // Compliant
    char[] passwordFromGetPwd = getPwd(""); // Compliant

    String CA_PASSWORD = "ca-password"; // Compliant
    String caPassword = CA_PASSWORD; // Compliant

    final String MY_PASSWORD = "1234"; // Noncompliant
    String params = "user=admin&password=Password123"; // Noncompliant
    String sqlserver = "pgsql:host=localhost port=5432 dbname=test user=postgres password=postgres"; // Noncompliant

    String variableNameWithPasswordInIt = "xxx"; // Noncompliant [[sc=12;ec=40]]
    String variableNameWithPasswordInItEmpty = "";
    String variableNameWithPasswordInItOneChar = "X";
    String variableNameWithPasswordInItAnonymous = "anonymous";
    String variableNameWithPassphraseInIt = "xxx"; // Noncompliant
    String variableNameWithPasswdInIt = "xxx"; // Noncompliant [[sc=12;ec=38]]
    String variableNameWithPwdInIt = "xxx"; // Noncompliant [[sc=12;ec=35]]
    String otherVariableNameWithPasswordInIt;
    fieldNameWithPasswordInIt = "xx"; // Noncompliant
    fieldNameWithPasswordInIt = "X";
    fieldNameWithPasswordInIt = "anonymous";
    fieldNameWithPasswordInIt = retrievePassword();
    this.fieldNameWithPasswordInIt = "xx"; // Noncompliant
    this.fieldNameWithPasswordInIt = retrievePassword();
    variable1 = "xx";

    char[] passphrase = "whatever".toCharArray(); // Noncompliant
    passphrase = "whatever".toCharArray(); // Noncompliant
    passphrase = PASSED.toCharArray(); // Noncompliant
    passphrase = "".toCharArray();
    passphrase = "X".toCharArray();
    passphrase = "anonymous".toCharArray();

    String password = "123"; // Noncompliant
    if(password.equals("whatever")) { // Noncompliant
    }
    if("whatever".equals(password)) { // Noncompliant
    }
    if(PASSED.equals(password)) { // Noncompliant
    }
    if(password.equals("X")) {
    }
    if(password.equals("anonymous")) {
    }
    if(password.equals("password")) {
    }
    if("password".equals(password)) {
    }
    if(password.equals("password-1234")) {
    }
    if(password.equals("")) {
    }
    if(password.equals(null)) {
    }
    if(password.equals(EMPTY)) {
    }
    if("".equals(password)) {
    }
    if(equals(password)) {
    }

    java.sql.Connection conn = DriverManager.getConnection("jdbc:mysql://xxx/", "root", var);
    conn = DriverManager.getConnection("jdbc:mysql://xxx/", "root", "whateverpassword"); // Noncompliant
    conn = DriverManager.getConnection("jdbc:mysql://xxx/", "root", PASSED); // Noncompliant
    conn = DriverManager.getConnection("jdbc:mysql://xxx/");
    conn = DriverManager.getConnection("jdbc:db2://myhost:5021/mydb:user=dbadm;password=foo"); // Noncompliant

    String[] array = {};
    array[0] = "xx";

    A myA = new A();
    myA.setProperty("password", "xxxxx"); // Noncompliant
    myA.setProperty("passwd", "xxxxx"); // Noncompliant
    myA.setProperty("pwd", "xxxxx"); // Noncompliant
    myA.setProperty("pwd", "X");
    myA.setProperty("pwd", "anonymous");
    myA.setProperty("password", new Object());
    myA.setProperty("xxxxx", "password");
    myA.setProperty(12, "xxxxx");
    myA.setProperty(new Object(), new Object());
    myA.setProperty("password", "password"); // Compliant
    myA.setProperty("password", "pwd"); // Compliant
    myA.setProperty("something", "else").setProperty("password", "xxxxx"); // Noncompliant [[sc=42;ec=53]]

    PasswordAuthentication pa;
    pa = new PasswordAuthentication("userName", "1234".toCharArray());  // Noncompliant {{Remove this hard-coded password.}}
    pa = new PasswordAuthentication("userName", var.toCharArray());
    pa = new PasswordAuthentication("userName", pwd); // Compliant
    pa = new PasswordAuthentication("userName", getPwd(var)); // Compliant
    pa = new PasswordAuthentication("userName", var.toCharArray()); // Compliant

    OtherPasswordAuthentication opa;
    opa = new OtherPasswordAuthentication("userName", "1234".toCharArray()); // Compliant

    java.util.Properties props = new java.util.Properties();
    props.put(Context.SECURITY_CREDENTIALS, "1234"); // Noncompliant
    props.put("java.naming.security.credentials", "1234"); // Noncompliant
    props.put("password", "whateverpassword"); // Compliant

    java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
    env.put(Context.SECURITY_CREDENTIALS, "1234"); // Noncompliant
    env.put("", "1234");
    env.put(Context.SECURITY_CREDENTIALS, "");
    env.put("password", "whateverpassword"); // Compliant

    String[] urls = {
      "http://user:123456@server.com/path",     // Noncompliant {{Review this hard-coded URL, which may contain a password.}}
      "ftp://anonymous:anonymous@wustl.edu",    // OK, user == password
      "http://admin:admin@server.com/path",     // OK, user == password
      "http://user:@server.com/path",           // OK, password is empty
      "http://user@server.com/path",            // OK, no password
      "https://server:80/path",                 // OK, no user and password
      "http://server.com/path",                 // OK, no user and password
      "http://:123456@server.com/path",         // Noncompliant
      "HTTPS://:token0932448209@server.com",    // Noncompliant
      "https://invalid::url::format",
      "too-long-url-scheme://user:123456@server.com",
    };
  }

  private char[] getPwd(String s) {
    return null;
  }

  private String retrievePassword() {
    return null;
  }

  static class A {
    private A setProperty(Object property, Object Value) {
      return this;
    }
  }


  private static class OtherPasswordAuthentication {
    OtherPasswordAuthentication(String username, char[] pwd) {}
  }
}
