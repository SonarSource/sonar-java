package checks.security;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

class Main {

  public static void main(String[] args) {

    X509TrustManager trustManager = new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { // Compliant, to avoid FP, assumes it throws exceptions
        unResolvedMethod();
      }

      @Override
      public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { // Noncompliant
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }
    };

    X509ExtendedTrustManager extendedManager = new EmptyX509ExtendedTrustManager();
  }
}
