package checks;

import java.net.PasswordAuthentication;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.naming.Context;

/**
 * This check detect hardcoded password in multiples cases:
 * - 1. String literal
 * - 1.1 Urls
 * - 2. Variable declaration
 * - 3. Assignment
 * - 4. Method invocations
 * - 4.1 Equals
 * - 4.2 API setting password
 * - 4.3 Setting password
 * - 5. Constructors
 */
class HardCodedPasswordCheckSample {

  String fieldNameWithPasswordInIt = retrievePassword();

  private static final String PASSED = "passed"; // compliant nothing to do with passwords
  private static final String EMPTY = "";

  private void a(char[] pwd, String var) throws SQLException {
    // ========== 1. String literal ==========
    // The variable name does not influence the issue, only the string is considered.
    String variable1 = "blabla";
    String variable2 = "login=a&password=xxx"; // Compliant, short value filter
    String variable2_2 = "login=a&password=xvxf6_gaa"; // Noncompliant {{'password' detected in this expression, review this potentially hard-coded password.}}
//                       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    String variable3 = "login=a&passwd=xxx"; // Compliant: short value filter
    String variable3_2 = "login=a&passwd=xvxf6_gaa"; // Noncompliant {{'passwd' detected in this expression, review this potentially hard-coded password.}}
    String variable4 = "login=a&pwd=xxx"; // Compliant, short value filter
    String variable4_2 = "login=a&pwd=xvxf6_gaa"; // Noncompliant {{'pwd' detected in this expression, review this potentially hard-coded password.}}
    String variable5 = "login=a&password=";
    String variable6 = "login=a&password= ";
    String params = "user=admin&password=Password123"; // Compliant, value contains the keyword password
    String params2 = "user=admin&password=sonar123"; // Noncompliant
    String sqlserver = "pgsql:host=localhost port=5432 dbname=test user=postgres password=postgres"; // Noncompliant

    String query1 = "password=?hard-to-find"; // Noncompliant
    String query1_1 = "password=???"; // Compliant
    // Password starting with "\"" are ignored
    String query7 = "\"password=\""; // Compliant
    // Password starting with ":" are reported
    String query2 = "password=:password"; // Noncompliant
    String query3 = "password=:param"; // Noncompliant
    // Password containing "%s" are ignored
    String query5 = "password=%s"; // Compliant
    String query6 = "password=\"%s\""; // Compliant
    // Password shorter than 2 characters are ignored
    String query1_2 = "password=X"; // Compliant
    // "anonymous" is not explicitly ignored any more
    String query1_3 = "password=anonymous"; // Noncompliant
    // Not hardcoded
    String query4 = "password='" + pwd + "'"; // Compliant

    // When the string is in a variable with a name containing password, we let the logic about variable declaration (2.) determine if we should raise an issue.
    String inPasswordConstant = "user=admin&password=Password123"; // Compliant

    // Password are correctly extracted
    String query10 = "password=something&user=user"; // Noncompliant
    // "anonymous" is not explicitly ignored any more
    String query11 = "password=anonymous&user=user"; // Noncompliant
    String query17 = "password=anonymous,user=user"; // Noncompliant
    String query18 = "password=anonymous|user=user"; // Noncompliant
    String query12 = "password=anonymous user=user"; // Noncompliant
    String query121 = "password=anonymous;user=user"; // Noncompliant
    String query122 = "password=anonymous#user=user"; // Noncompliant
    String query13 = "password=anonymous\tuser=user"; // Noncompliant
    String query14 = "password=anonymous\nuser=user"; // Noncompliant
    String query15 = "password=something&user=user%s"; // Noncompliant
    // Password starting with ":" are reported
    String query16 = "passwordProtected password=:notAPassword"; // Noncompliant

    // ========== 1.2 Urls ==========
    // Exclusions also apply when the password if found in an url
    String[] urls = {
      "http://user:123456@server.com/path", // Compliant, fake value filter
      "http://user:xvxf6_gaa@server.com/path", // Noncompliant {{Review this hard-coded URL, which may contain a password.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      "ftp://anonymous:anonymous@wustl.edu",    // OK, user == password
      "ftp://:anonymous@wustl.edu", // Noncompliant
      "http://admin:admin@server.com/path",     // OK, user == password
      "http://user:@server.com/path",           // OK, password is empty
      "http://user@server.com/path",            // OK, no password
      "https://server:80/path",                 // OK, no user and password
      "http://server.com/path",                 // OK, no user and password
      "http://:123456@server.com/path", // Compliant, fake value filter
      "http://:xvxf6_gaa@server.com/path", // Noncompliant
      "HTTPS://:token0932448209@server.com", // Noncompliant
      "https://invalid::url::format",
      "too-long-url-scheme://user:123456@server.com",
    };

    // ========== 2. Variable declaration ==========
    // The variable name should contain a password word
    final String MY_PASSWORD = "1234"; // Compliant, fake value filter
    final String MY_PASSWORD_2 = "xvxf6_gaa"; // Noncompliant
    String variableNameWithPasswordInIt = "1234"; // Compliant, fake value filter
    String variableNameWithPasswordInIt2 = "xvxf6_gaa"; // Noncompliant {{'Password' detected in this expression, review this potentially hard-coded password.}}
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    String variableNameWithPassphraseInIt = "xxx"; // Compliant, short value filter
    String variableNameWithPassphraseInIt2 = "xvxf6_gaa"; // Noncompliant {{'Passphrase' detected in this expression, review this potentially hard-coded password.}}
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    String variableNameWithPasswdInIt = "xxx"; // Compliant, short value filter
    String variableNameWithPasswdInIt2 = "xvxf6_gaa"; // Noncompliant {{'Passwd' detected in this expression, review this potentially hard-coded password.}}
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    String variableNameWithPwdInIt = "xxx"; // Compliant, short value filter
    String variableNameWithPwdInIt2 = "xvxf6_gaa"; // Noncompliant {{'Pwd' detected in this expression, review this potentially hard-coded password.}}
//         ^^^^^^^^^^^^^^^^^^^^^^^^
    String passwordToString = "SuperSecure".toString(); // Compliant, FN, but will be reported when we remove "toString()"
    // Same constraints when "toCharArray" is called on the password
    char[] passwordToChar = "SuperSecure".toCharArray(); // Noncompliant
//         ^^^^^^^^^^^^^^
    // Short passwords are ignored
    String variableNameWithPasswordInItEmpty = ""; // Compliant
    String variableNameWithPasswordInItOneChar = "X"; // Compliant
    char[] smallPasswordToChar = "x".toCharArray(); // Compliant

    // When the password contains a password word, we consider it as constant used to avoid duplicated string
    String passwordConst = "Password"; // Compliant
    String pwds = "pwd"; // Compliant
    final String PASSWORD = "Password"; // Compliant
    final String PASSWORD_INPUT = "[id='password']"; // Compliant
    final String PASSWORD_PROPERTY = "custom.password"; // Compliant
    final String TRUSTSTORE_PASSWORD = "trustStorePassword"; // Compliant
    final String CONNECTION_PASSWORD = "connection.password"; // Compliant
    final String RESET_PASSWORD = "/users/resetUserPassword"; // Compliant
    final String RESET_PWD = "/users/resetUserPassword"; // Compliant
    char[] passwordToChar2 = "password".toCharArray(); // Compliant
    char[] passwordToChar3 = "http-password".toCharArray(); // Compliant
    String passwordToString2 = "http-password".toString(); // Compliant
    char[] passwordFromGetPwd = getPwd(""); // Compliant
    String CA_PASSWORD = "ca-password"; // Compliant
    String caPassword = CA_PASSWORD; // Compliant
    // When the declaration has no initializer, we don't report any issue
    String otherVariableNameWithPasswordInIt;

    // ========== 3. Assignment ==========
    // The variable name should contain a password word
    fieldNameWithPasswordInIt = "xx"; // Compliant, short value filter
    fieldNameWithPasswordInIt = "xvxf6_gaa"; // Noncompliant {{'Password' detected in this expression, review this potentially hard-coded password.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^
    this.fieldNameWithPasswordInIt = "xx"; // Compliant, short value filter
    this.fieldNameWithPasswordInIt = "xvxf6_gaa"; // Noncompliant
    // Short password are ignored
    fieldNameWithPasswordInIt = "X";
    // "anonymous" is not explicitly ignored any more
    fieldNameWithPasswordInIt = "anonymous"; // Noncompliant
    // Not a hardcoded password
    fieldNameWithPasswordInIt = retrievePassword();
    this.fieldNameWithPasswordInIt = retrievePassword();
    // Not a password name
    variable1 = "xx";
    // Same constraint when "toCharArray" is called on the password
    char[] passphrase = "whatever".toCharArray(); // Compliant, fake value filter
    passphrase = "xvxf6_gaa".toCharArray(); // Noncompliant
    passphrase = "whatever".toCharArray(); // Compliant, fake value filter
    passphrase = "xvxf6_gaa".toCharArray(); // Noncompliant
    passphrase = PASSED.toCharArray(); // Noncompliant
    passphrase = "".toCharArray();
    passphrase = "X".toCharArray();
    // Contains a fake password keyword: password
    fieldNameWithPasswordInIt = "password"; // Compliant

    // ========== 4. Method invocations ==========
    // ========== 4.1 Equals ==========
    // When one side of the equals contains a password word, report an issue
    String password = "123"; // Compliant, short value filter
    if(password.equals("whatever")) { // Compliant, fake value filter

    }
    String password2 = "xvxf6_gaa"; // Noncompliant
    if(password2.equals("xvxf6_gaa")) { // Noncompliant
//     ^^^^^^^^^
    }
    if("whatever".equals(password)) { // Compliant, fake value filter

    }
    if("xvxf6_gaa".equals(password)) { // Noncompliant
//                        ^^^^^^^^
    }
    if(PASSED.equals(password)) { // Noncompliant
    }
    // Short password are ignored
    if(password.equals("X")) {
    }
    if(password.equals("")) {
    }
    if("".equals(password)) {
    }
    // When the actual password contains a password word, we don't report an issue
    if(password.equals("password")) {
    }
    if("password".equals(password)) {
    }
    if(password.equals("password-1234")) {
    }
    // Corner cases
    if(password.equals(null)) {
    }
    if(password.equals(EMPTY)) {
    }
    if(equals(password)) {
    }

    // ========== 4.2 API setting password ==========
    // The second argument of "getConnection" contains a password: report an issue if it is hardcoded
    java.sql.Connection conn = DriverManager.getConnection("jdbc:mysql://xxx/", "root", var);
    conn = DriverManager.getConnection("jdbc:mysql://xxx/", "root", "whateverpassword"); // Compliant, handled by S6437
    conn = DriverManager.getConnection("jdbc:mysql://xxx/", "root", PASSED); // Compliant, handled by S6437
    conn = DriverManager.getConnection("jdbc:mysql://xxx/");
    // Password not set as argument, but it is still detected in the string itself is detected thanks to (1.)
    conn = DriverManager.getConnection("jdbc:db2://myhost:5021/mydb:user=dbadm;password=foo"); // Compliant, short value filter
    conn = DriverManager.getConnection("jdbc:db2://myhost:5021/mydb:user=dbadm;password=xvxf6_gaa"); // Noncompliant
//                                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    // ========== 4.3 Setting password ==========
    // When a method call has two arguments potentially containing String, we report an issue the same way we would with a variable declaration
    A myA = new A();
    myA.setProperty("password", "xxxxx"); // Compliant, short value filter
    myA.setProperty("password", "xvxf6_gaa"); // Noncompliant
    myA.setProperty("passwd", "xxxxx"); // Compliant, short value filter
    myA.setProperty("passwd", "xvxf6_gaa"); // Noncompliant
    myA.setProperty("pwd", "xxxxx"); // Compliant, short value filter
    myA.setProperty("pwd", "xvxf6_gaa"); // Noncompliant
    // Short password are ignored
    myA.setProperty("pwd", "X");
    // "anonymous" is not explicitly ignored any more
    myA.setProperty("pwd", "anonymous"); // Noncompliant
    // Not hardcoded
    myA.setProperty("password", new Object());
    // We only consider the second argument as containing a password
    myA.setProperty("xxxxx", "password");
    // Exclude when the password word is contain into the expected password
    myA.setProperty("password", "password"); // Compliant
    myA.setProperty("password", "pwd"); // Compliant
    // Other test cases
    myA.setProperty(12, "xxxxx");
    myA.setProperty(new Object(), new Object());
    myA.setProperty("something", "else").setProperty("password", "xxxxx"); // Compliant, short/repeated value filter
    myA.setProperty("something", "else").setProperty("password", "xvxf6_gaa"); // Noncompliant
//                                       ^^^^^^^^^^^

    // Same for code not user defined
    java.util.Properties props = new java.util.Properties();
    props.put(Context.SECURITY_CREDENTIALS, "1234"); // Compliant, short value filter
    props.put(Context.SECURITY_CREDENTIALS, "xvxf6_gaa"); // Noncompliant
//        ^^^
    props.put("java.naming.security.credentials", "1234"); // Compliant, short value filter
    props.put("java.naming.security.credentials", "xvxf6_gaa"); // Noncompliant
    props.put("password", "whateverpassword"); // Compliant

    java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
    env.put(Context.SECURITY_CREDENTIALS, "1234"); // Compliant, short value filter
    env.put(Context.SECURITY_CREDENTIALS, "xvxf6_gaa"); // Noncompliant
    env.put("", "xvxf6_gaa");
    env.put(Context.SECURITY_CREDENTIALS, "");
    env.put("password", "whateverpassword"); // Compliant

    // Others tests...
    env.put("password", OtherPasswordAuthentication.get());

    // ========== 5. Constructors ==========
    // Second argument of "PasswordAuthentication" is setting explicitly a password
    PasswordAuthentication pa;

    pa = new PasswordAuthentication("userName", "1234".toCharArray()); // Compliant, handled by S6437
    // Not hardcoded
    pa = new PasswordAuthentication("userName", var.toCharArray());
    pa = new PasswordAuthentication("userName", pwd); // Compliant
    pa = new PasswordAuthentication("userName", getPwd(var)); // Compliant
    pa = new PasswordAuthentication("userName", var.toCharArray()); // Compliant

    OtherPasswordAuthentication opa;
    opa = new OtherPasswordAuthentication("userName", "1234".toCharArray()); // Compliant
    String[] array = {};
    array[0] = "xx";
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

    static String get() {
      return "";
    }
  }
}
