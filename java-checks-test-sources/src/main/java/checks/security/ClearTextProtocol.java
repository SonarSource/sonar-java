package checks.security;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPSClient;
import org.apache.commons.net.telnet.TelnetClient;

public class ClearTextProtocol {

  void noncompliant() {
    new FTPClient(); // Noncompliant [[sc=9;ec=18]] {{Using FTP protocol is insecure. Use SFTP, SCP or FTPS instead.}}
    new TelnetClient(); // Noncompliant {{Using Telnet protocol is insecure. Use SSH instead.}}
    new SMTPClient(); // Noncompliant {{Using clear-text SMTP protocol is insecure. Use SMTP over SSL/TLS or SMTP with STARTTLS instead.}}
    new SMTPClient("UTF-8"); // Noncompliant {{Using clear-text SMTP protocol is insecure. Use SMTP over SSL/TLS or SMTP with STARTTLS instead.}}
  }

  void compliant() {
    new FTPSClient();
    new SMTPSClient();
  }

}
