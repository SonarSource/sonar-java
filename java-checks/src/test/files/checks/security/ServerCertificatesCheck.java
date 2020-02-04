import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    trustManager = new X509TrustManager() {
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

  }
}

interface Coverage {
  void method();
}
