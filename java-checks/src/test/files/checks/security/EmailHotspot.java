import javax.mail.*;
import javax.mail.internet.MimeMessage;

public class Main {
  public static void sendEmail (Session session, String subject) throws MessagingException{
    Message message = new MimeMessage(session);  // Noncompliant {{Make sure that this email is sent in a safe manner.}}

    // For example the setSubject method is vulnerable to Header injection before
    // version 1.5.6 of javamail
    message.setSubject(subject);
    // ...
  }
}
