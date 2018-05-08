import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrustAllManager implements X509TrustManager {

  private static Logger LOG = Logger.getLogger("TrustAllManager");

  @Override
  public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException { // Noncompliant {{Change this method so it throws exceptions.}}
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
      public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        java.lang.Integer.parseInt("error");
      }

      @Override
      public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        new java.io.FileInputStream(null);
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }
    };

    trustManager = new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
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

  }
}

interface Coverage {
  void method();
}
