package checks;

import java.net.PasswordAuthentication;

class HardCodedPasswordCheckCustom {

  String fieldNameWithPasswordInIt;
  String fieldNameWithBazookaInIt;

  private void a(char[] pwd, String var) {
    String variable2 = "login=a&password=xvxf6_gaa"; // Compliant
    String variable3 = "login=a&passwd=xvxf6_gaa"; // Compliant
    String variable4 = "login=a&pwd=xvxf6_gaa"; // Compliant
    String variable5 = "login=a&marmalade=xxx"; // Compliant, short value filter
    String variable5_2 = "login=a&marmalade=xvxf6_gaa"; // Noncompliant {{'marmalade' detected in this expression, review this potentially hard-coded password.}}
//                       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    String variable6 = "login=a&bazooka=xxx"; // Compliant, short value filter
    String variable6_2 = "login=a&bazooka=xvxf6_gaa"; // Noncompliant

    String variableNameWithBazookaInIt = "xxx"; // Compliant, short value filter
    String variableNameWithBazookaInIt_2 = "xvxf6_gaa"; // Noncompliant
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    String variableNameWithmarMalAdeInIt = "xvxf6_gaa"; // Noncompliant
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    String variableNameWithPwdInIt = "xvxf6_gaa"; // Compliant
    String otherVariableNameWithPasswordInIt;
    fieldNameWithPasswordInIt = "xx";
    this.fieldNameWithBazookaInIt = "xx"; // Compliant, , short value filter


    HardCodedPasswordCheckCustom myA = new HardCodedPasswordCheckCustom();
    myA.setProperty("marmalade", "xxxxx"); // Compliant, short value filter
    myA.setProperty("marmalade", "xvxf6_gaa"); // Noncompliant
    myA.setProperty("marmalade", "marmalade"); // Compliant
    myA.setProperty("passwd", "xvxf6_gaa"); //Compliant
    myA.setProperty("pwd", "xvxf6_gaa"); // Compliant


    new PasswordAuthentication("userName", "xvxf6_gaa".toCharArray()); // Compliant, handled by S6437
    new PasswordAuthentication("userName", pwd); // Compliant
    new PasswordAuthentication("userName", getPwd(var)); // Compliant
    new PasswordAuthentication("userName", var.toCharArray()); // Compliant

    new OtherPasswordAuthentication("userName", "xvxf6_gaa".toCharArray()); // Compliant
  }

  private void setProperty(Object property, Object Value) { }
  private static char[] getPwd(String var) { return null; }

  private static class OtherPasswordAuthentication {
    OtherPasswordAuthentication(String username, char[] pwd) {}
  }
}
