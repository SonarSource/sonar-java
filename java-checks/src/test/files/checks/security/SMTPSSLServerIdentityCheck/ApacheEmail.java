import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;

public class ApacheEmail {

  static {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(true);  // invocation outside a method
  }

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

  public void foo5(boolean flag) {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(flag);     // Compliant needs SE - engine to compute flag's value
  }

  public void foo6(boolean flag) {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(true);  // Compliant needs SE - engine to compute flag's value
    email.setSSLCheckServerIdentity(flag);
  }

  public void foo7(boolean flag) {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(flag);  // Compliant
    email.setSSLCheckServerIdentity(true);
  }

  public void foo8() {
    Email email = new SimpleEmail();
    email.setSSL(true);   // Noncompliant
  }

  public void foo9() {
    Email email = new SimpleEmail();
    email.setTLS(true);   // Noncompliant
  }

  public void foo10() {
    Email email = new SimpleEmail();
    email.setStartTLSEnabled(true);   // Noncompliant
  }

  public void foo11() {
    Email email = new SimpleEmail();
    email.setStartTLSRequired(true);   // Noncompliant
  }
}
interface Test {
  java.util.function.Supplier<Object> s = () -> {
    Email email = new SimpleEmail();
    email.setSSLOnConnect(true);
    return email;
  };
}
