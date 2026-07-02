package checks;

import java.net.PasswordAuthentication;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.naming.Context;

class HardCodedPasswordCheckSample {

  private void a(char[] pwd, String var) throws SQLException {
    MyUnknownClass.myUnknownMethod("password", "xvxf6_gaa"); // Noncompliant
    MyUnknownClass.myUnknownMethod("other", "xvxf6_gaa");    // Compliant
  }

}
