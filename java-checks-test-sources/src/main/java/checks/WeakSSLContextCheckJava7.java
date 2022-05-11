package checks;

import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;

class WeakSSLContextCheckJava7 {

  void foo() throws NoSuchAlgorithmException {
    bar(SSLContext.getInstance("TLS")); // Noncompliant
    bar(SSLContext.getInstance("DTLS")); // Noncompliant
  }

  void bar(SSLContext ctx) {
    System.out.println(ctx);
  }
}
