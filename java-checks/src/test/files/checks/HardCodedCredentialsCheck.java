import java.net.PasswordAuthentication;

class A {

  String fieldNameWithPasswordInIt = retrievePassword();

  private void a(char[] pwd, String var) {
    String variable1 = "blabla";
    String variable2 = "login=a&password=xxx"; // Noncompliant [[sc=24;ec=46]] {{Remove this hard-coded password.}}
    String variable3 = "login=a&passwd=xxx"; // Noncompliant
    String variable4 = "login=a&pwd=xxx"; // Noncompliant
    String variable5 = "login=a&password=";
    String variable6 = "login=a&password= ";

    String variableNameWithPasswordInIt = "xxx"; // Noncompliant [[sc=12;ec=40]]
    String variableNameWithPasswdInIt = "xxx"; // Noncompliant [[sc=12;ec=38]]
    String variableNameWithPwdInIt = "xxx"; // Noncompliant [[sc=12;ec=35]]
    String otherVariableNameWithPasswordInIt;
    fieldNameWithPasswordInIt = "xx"; // Noncompliant
    fieldNameWithPasswordInIt = retrievePassword();
    this.fieldNameWithPasswordInIt = "xx"; // Noncompliant
    this.fieldNameWithPasswordInIt = retrievePassword();
    variable1 = "xx";

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
    pa = new PasswordAuthentication("userName", "1234".toCharArray());  // Noncompliant
    pa = new PasswordAuthentication("userName", pwd); // Compliant
    pa = new PasswordAuthentication("userName", getPwd(var)); // Compliant
    pa = new PasswordAuthentication("userName", var.toCharArray()); // Compliant

    OtherPasswordAuthentication opa;
    opa = new OtherPasswordAuthentication("userName", "1234".toCharArray()); // Compliant
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
