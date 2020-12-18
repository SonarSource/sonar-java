package checks.security;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

class TrustAllManager implements X509TrustManager {

  private static Logger LOG = Logger.getLogger("TrustAllManager");

  @Override
  public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { // Noncompliant {{Enable server certificate validation on this SSL/TLS connection.}}
  }

  @Override
  public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { // Noncompliant
    LOG.log(Level.SEVERE, "ERROR " + s);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return null;
  }
}

class Main {

  public static void main(String[] args) {

    X509TrustManager trustManager = new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { // Noncompliant
      }

      @Override
      public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { // Noncompliant
        System.out.println("123");
      }

      // does not override
      public void checkServerTrusted(String s) {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }
    };

    trustManager = new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        throw new CertificateException();
      }

      @Override
      public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        checkClientTrusted(x509Certificates, s);
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }
    };

    trustManager = new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { // Compliant, FN, it throws another kind of Exception
        java.lang.Integer.parseInt("error");
      }

      @Override
      public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { // Compliant, FN, Exception is catched
        try {
          throw new CertificateException();
        } catch (CertificateException e) {
          e.printStackTrace();
        }
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }
    };

    X509ExtendedTrustManager extendedManager = new EmptyX509ExtendedTrustManager();
  }
}
class EmptyX509ExtendedTrustManager extends X509ExtendedTrustManager {
  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {} // Noncompliant An empty implementation is not considered valid

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {} // Noncompliant An empty implementation is not considered valid

  @Override
  public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {} // Noncompliant An empty implementation is not considered valid

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {} // Noncompliant An empty implementation is not considered valid

  @Override
  public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {} // Noncompliant An empty implementation is not considered valid

  @Override
  public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {} // Noncompliant An empty implementation is not considered valid

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[0];
  }
}

interface Coverage {
  void method();
}
