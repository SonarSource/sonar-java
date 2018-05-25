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

  public void foo4() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // Noncompliant {{Enable server identity validation, set "mail.smtp.ssl.checkserveridentity" to true}}
    props.put("mail.smtp.ssl.checkserveridentity", false);
  }

  public void false_positive(boolean flag) {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.ssl.checkserveridentity", flag);  // Compliant FN
  }
}
