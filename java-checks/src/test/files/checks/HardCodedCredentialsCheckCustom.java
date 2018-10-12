import java.net.PasswordAuthentication;

class A {

  private void a(char[] pwd, String var) {
    String variable2 = "login=a&password=xxx"; // Compliant
    String variable3 = "login=a&passwd=xxx"; // Compliant
    String variable4 = "login=a&pwd=xxx"; // Compliant
    String variable5 = "login=a&marmalade=xxx"; // Noncompliant [[sc=24;ec=47]] {{'marmalade' detected in this expression, review this potentially hard-coded credential.}}
    String variable6 = "login=a&bazooka=xxx "; // Noncompliant

    String variableNameWithBazookaInIt = "xxx"; // Noncompliant [[sc=12;ec=39]]
    String variableNameWithmarMalAdeInIt = "xxx"; // Noncompliant [[sc=12;ec=41]]
    String variableNameWithPwdInIt = "xxx"; // Compliant
    String otherVariableNameWithPasswordInIt;
    fieldNameWithPasswordInIt = "xx"; // Compliant
    this.fieldNameWithBazookaInIt = "xx"; // Noncompliant


    A myA = new A();
    myA.setProperty("marmalade", "xxxxx"); // Noncompliant
    myA.setProperty("passwd", "xxxxx"); //Compliant
    myA.setProperty("pwd", "xxxxx"); // Compliant


    new PasswordAuthentication("userName", "1234".toCharArray());  // Noncompliant {{Remove this hard-coded password.}}
    new PasswordAuthentication("userName", pwd); // Compliant
    new PasswordAuthentication("userName", getPwd(var)); // Compliant
    new PasswordAuthentication("userName", var.toCharArray()); // Compliant

    new OtherPasswordAuthentication("userName", "1234".toCharArray()); // Compliant
  }

  private void setProperty(Object property, Object Value) {
  }

  private static class OtherPasswordAuthentication {
    OtherPasswordAuthentication(String username, char[] pwd) {}
  }
}
