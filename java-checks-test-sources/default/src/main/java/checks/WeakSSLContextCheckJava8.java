package checks;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.net.ssl.SSLContext;

class WeakSSLContextCheckJava8 {
  private static final char ONE = '1';

  void characterProtocols() throws NoSuchAlgorithmException {
    SSLContext.getInstance("TLSv" + ONE); // Noncompliant {{Change this code to use a stronger protocol.}}
//                         ^^^^^^^^^^^^
    SSLContext.getInstance("SS" + 'L'); // Noncompliant {{Change this code to use a stronger protocol.}}
//                         ^^^^^^^^^^
    SSLContext.getInstance("TLSv" + ONE + ".2");
    SSLContext.getInstance('T' + "LSv1.3");
  }

  void foo(String protocol, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
    bar(SSLContext.getInstance(protocol));

    bar(SSLContext.getInstance("SSL")); // Noncompliant {{Change this code to use a stronger protocol.}}
//                             ^^^^^
    bar(SSLContext.getInstance("SSLv2")); // Noncompliant
    bar(SSLContext.getInstance("SSLv3")); // Noncompliant
    bar(SSLContext.getInstance("TLS")); // compliant, refer to latest version of protocol
    bar(SSLContext.getInstance("TLSv1")); // Noncompliant
    bar(SSLContext.getInstance("TLSv1.1")); // Noncompliant
    bar(SSLContext.getInstance("TLSv1.2"));
    bar(SSLContext.getInstance("TLSv1.3"));
    bar(SSLContext.getInstance("DTLS"));
    bar(SSLContext.getInstance("DTLSv1.0")); // Noncompliant
    bar(SSLContext.getInstance("DTLSv1.2"));
    bar(SSLContext.getInstance("DTLSv1.3"));

    bar(SSLContext.getInstance("SSL", provider)); // Noncompliant
    bar(SSLContext.getInstance("TLSv1.2", provider));
    bar(SSLContext.getInstance("TLSv1.2", "SSL"));
  }

  void bar(SSLContext ctx) {
    System.out.println(ctx);
  }

}
