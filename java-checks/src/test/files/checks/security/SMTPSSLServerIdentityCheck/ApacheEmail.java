import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

public class ApacheEmail {

  public void foo() {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(true);   // Noncompliant [[sc=5;ec=32]] {{Enable server identity validation on this SMTP SSL connection.}}
  }

  public void foo2() {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(true);  // Compliant
    email.setSSLCheckServerIdentity(true);
  }

  public void foo3() {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(false); // Compliant
    email.setSSLCheckServerIdentity(true);
  }

  public void foo4() {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(true);  // Noncompliant
    email.setSSLCheckServerIdentity(false);
  }

  public void false_negative1(boolean flag) {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(flag);     // Compliant FN flag value will be read with SE engine
  }

  public void foo5(boolean flag) {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(true);  // Compliant   needs SE - engine to compute flag's value
    email.setSSLCheckServerIdentity(flag);
  }

  public void foo5(boolean flag) {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(flag);  // Compliant
    email.setSSLCheckServerIdentity(true);
  }
}
