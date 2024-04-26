package checks.security.VerifiedServerHostnamesCheck;

import java.util.Properties;

public class JavaMailSession {

  public void foo() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // Noncompliant {{Enable server hostname verification on this SSL/TLS connection, by setting "mail.smtp.ssl.checkserveridentity" to true.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  public void foo2() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // Compliant
    props.put("mail.smtp.ssl.checkserveridentity", true);
  }

  public void foo3() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // Noncompliant {{Enable server hostname verification on this SSL/TLS connection, by setting "mail.smtp.ssl.checkserveridentity" to true.}}
    props.put("mail.smtp.ssl.checkserveridentity", false);
  }

  public void foo4(boolean flag) {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.ssl.checkserveridentity", flag);  // Compliant
  }

  public void parameters_dont_match() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "");
  }

  public void foo6() {
    Properties props = new Properties();
    props.put("mail.smtp.ssl.protocols", ""); // Compliant
  }

  public void foo7() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // Noncompliant
    props.clear();   // this invocation does not check server's identity
  }

  public void foo8(boolean cond) {
    if (cond) {
      Properties props = new Properties();
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // Noncompliant
    } else {
      Properties props = new Properties();
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // Compliant
      props.put("mail.smtp.ssl.checkserveridentity", true);
    }
  }

  public void foo9() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // Noncompliant
    anotherUnrelatedCall();
  }

  private void anotherUnrelatedCall() {

  }
}
