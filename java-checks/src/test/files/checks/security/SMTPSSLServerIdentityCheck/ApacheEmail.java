import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

public class ApacheEmail {

  public void foo() {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(true);
    email.send(); // Noncompliant [[sc=5;ec=15]] {{Enable server identity validation on this SMTP SSL connection.}}
  }

  public void foo2() {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(true);
    email.setSSLCheckServerIdentity(true);
    email.send();  // Compliant
  }

  public void foo3() {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(false);
    email.setSSLCheckServerIdentity(true);
    email.send();  // Noncompliant
  }

  public void foo4() {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(true);
    email.setSSLCheckServerIdentity(false);
    email.send();  // Noncompliant
  }

  public void false_negative1(boolean flag) {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(flag);
    email.send(); // Noncompliant FP flag value will be read with SE engine
  }
}
