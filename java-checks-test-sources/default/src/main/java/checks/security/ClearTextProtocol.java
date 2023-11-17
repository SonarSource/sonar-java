package checks.security;

import android.webkit.WebSettings;
import java.util.Arrays;
import java.util.Collections;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
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

  void okHttp() {
    OkHttpClient client1 = new OkHttpClient.Builder()
      .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT)) // Noncompliant [[sc=80;ec=89]] {{Using HTTP protocol is insecure. Use HTTPS instead.}}
      .build();

    OkHttpClient client15 = new OkHttpClient.Builder()
      .connectionSpecs(Collections.singletonList(ConnectionSpec.CLEARTEXT)) // Noncompliant [[sc=65;ec=74]] {{Using HTTP protocol is insecure. Use HTTPS instead.}}
      .build();

    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.CLEARTEXT) // Noncompliant [[sc=69;ec=78]] {{Using HTTP protocol is insecure. Use HTTPS instead.}}
      .build();

    OkHttpClient client2 = new OkHttpClient.Builder()
      .connectionSpecs(Collections.singletonList(spec))
      .build();

    // Compliant
    OkHttpClient client3 = new OkHttpClient.Builder()
      .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS)) // Compliant
      .build();

    OkHttpClient client4 = new OkHttpClient.Builder()
      .connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS)) // Compliant
      .build();

    ConnectionSpec spec2 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS) // Compliant
      .build();

    OkHttpClient client5 = new OkHttpClient.Builder()
      .connectionSpecs(Collections.singletonList(spec2))
      .build();
  }

  void androidWebSettings(WebSettings settings, int value) {
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW); // Noncompliant [[sc=34;ec=72]] {{Using a relaxed mixed content policy is security-sensitive.}}
    settings.setMixedContentMode(0); // Noncompliant
    settings.setMixedContentMode(value); // Compliant
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW); // Compliant
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE); // Compliant
  }

}
