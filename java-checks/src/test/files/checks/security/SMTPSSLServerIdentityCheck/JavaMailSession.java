import java.util.Properties;

public class JavaMailSession {

  public void foo() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");// Noncompliant [[sc=5;ec=81]] {{Enable server identity validation, set "mail.smtp.ssl.checkserveridentity" to true}}
  }

  public void foo2() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // Compliant
    props.put("mail.smtp.ssl.checkserveridentity", true);
  }

  public void foo3() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // Noncompliant {{Enable server identity validation, set "mail.smtp.ssl.checkserveridentity" to true}}
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
}
