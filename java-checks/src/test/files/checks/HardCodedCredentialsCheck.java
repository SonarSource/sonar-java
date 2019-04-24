import java.net.PasswordAuthentication;
import java.sql.DriverManager;
import javax.naming.Context;

class A {

  String fieldNameWithPasswordInIt = retrievePassword();

  private static final String PASSED = "passed"; // compliant nothing to do with passwords
  private static final String EMPTY = "";

  private void a(char[] pwd, String var) {
    String variable1 = "blabla";
    String variable2 = "login=a&password=xxx"; // Noncompliant [[sc=24;ec=46]] {{'password' detected in this expression, review this potentially hard-coded credential.}}
    String variable3 = "login=a&passwd=xxx"; // Noncompliant
    String variable4 = "login=a&pwd=xxx"; // Noncompliant
    String variable5 = "login=a&password=";
    String variable6 = "login=a&password= ";

    String variableNameWithPasswordInIt = "xxx"; // Noncompliant [[sc=12;ec=40]]
    String variableNameWithPasswordInItEmpty = "";
    String variableNameWithPassphraseInIt = "xxx"; // Noncompliant
    String variableNameWithPasswdInIt = "xxx"; // Noncompliant [[sc=12;ec=38]]
    String variableNameWithPwdInIt = "xxx"; // Noncompliant [[sc=12;ec=35]]
    String otherVariableNameWithPasswordInIt;
    fieldNameWithPasswordInIt = "xx"; // Noncompliant
    fieldNameWithPasswordInIt = retrievePassword();
    this.fieldNameWithPasswordInIt = "xx"; // Noncompliant
    this.fieldNameWithPasswordInIt = retrievePassword();
    variable1 = "xx";

    char[] passphrase = "whatever".toCharArray(); // Noncompliant
    passphrase = "whatever".toCharArray(); // Noncompliant
    passphrase = PASSED.toCharArray(); // Noncompliant
    passphrase = "".toCharArray();

    String password = "123"; // Noncompliant
    if(password.equals("whatever")) { // Noncompliant
    }
    if("whatever".equals(password)) { // Noncompliant
    }
    if(PASSED.equals(password)) { // Noncompliant
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

    // coverage
    new String() {
      public String() {
        char[] passphrase = toCharArray();
      }
    };

    Connection conn = DriverManager.getConnection("jdbc:mysql://xxx/", "root", var);
    conn = DriverManager.getConnection("jdbc:mysql://xxx/", "root", "whateverpassword"); // Noncompliant
    conn = DriverManager.getConnection("jdbc:mysql://xxx/", "root", PASSED); // Noncompliant
    conn = DriverManager.getConnection("jdbc:mysql://xxx/", "root");
    conn = DriverManager.getConnection("jdbc:mysql://xxx/");
    conn = DriverManager.getConnection("jdbc:db2://myhost:5021/mydb:user=dbadm;password=foo"); // Noncompliant

    String[] array = {};
    array[0] = "xx";

    A myA = new A();
    myA.setProperty("password", "xxxxx"); // Noncompliant
    myA.setProperty("passwd", "xxxxx"); // Noncompliant
    myA.setProperty("pwd", "xxxxx"); // Noncompliant
    myA.setProperty("password", new Object());
    myA.setProperty("xxxxx", "password");
    myA.setProperty(12, "xxxxx");
    myA.setProperty(new Object(), new Object());

    MyUnknownClass.myUnknownMethod("password", "xxxxx"); // Noncompliant

    PasswordAuthentication pa;
    pa = new PasswordAuthentication("userName", "1234".toCharArray());  // Noncompliant {{Remove this hard-coded password.}}
    pa = new PasswordAuthentication("userName", var.toCharArray());
    pa = new PasswordAuthentication("userName", pwd); // Compliant
    pa = new PasswordAuthentication("userName", getPwd(var)); // Compliant
    pa = new PasswordAuthentication("userName", var.toCharArray()); // Compliant

    OtherPasswordAuthentication opa;
    opa = new OtherPasswordAuthentication("userName", "1234".toCharArray()); // Compliant

    Properties props = new Properties();
    props.put(Context.SECURITY_CREDENTIALS, "whateverpassword"); // Noncompliant
    props.put("java.naming.security.credentials", "whateverpassword"); // Noncompliant

    Hashtable<String, String> env = new Hashtable<String, String>();
    env.put(Context.SECURITY_CREDENTIALS, "whateverpassword"); // Noncompliant
    env.put("", "whateverpassword");
    env.put(Context.SECURITY_CREDENTIALS, "");
  }

  private char[] getPwd(String s) {
    return null;
  }

  private String retrievePassword() {
    return null;
  }

  private void setProperty(Object property, Object Value) {
  }

  private static class OtherPasswordAuthentication {
    OtherPasswordAuthentication(String username, char[] pwd) {}
  }
}
