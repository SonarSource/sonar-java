import java.util.Properties;
import javax.mail.Session;

public class JavaMailSession {

  public void foo() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    Session s = Session.getDefaultInstance(props, new javax.mail.Authenticator() { // Noncompliant [[sc=17;ec=43]] {{Enable server identity validation, set "mail.smtp.ssl.checkserveridentity" to true}}
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("username@gmail.com", "password");
      }
    });
  }

  public void foo2() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.ssl.checkserveridentity", true);
    Session s = Session.getDefaultInstance(props, new javax.mail.Authenticator() { // Compliant
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("username@gmail.com", "password");
      }
    });
  }

  public void foo3() {
    Properties props = new Properties();
    props.put("mail.smtp.ssl.checkserveridentity", true);
    Session s = Session.getDefaultInstance(props, new javax.mail.Authenticator() { // Noncompliant
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("username@gmail.com", "password");
      }
    });
  }

  public void foo4() {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.ssl.checkserveridentity", false);
    Session s = Session.getDefaultInstance(props, new javax.mail.Authenticator() { // Noncompliant {{Enable server identity validation, set "mail.smtp.ssl.checkserveridentity" to true}}
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("username@gmail.com", "password");
      }
    });
  }

  public void false_positive(boolean flag) {
    Properties props = new Properties();
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.ssl.checkserveridentity", flag);
    Session s = Session.getDefaultInstance(props, new javax.mail.Authenticator() { // Noncompliant {{Enable server identity validation, set "mail.smtp.ssl.checkserveridentity" to true}}
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication("username@gmail.com", "password");
      }
    });
  }
}
