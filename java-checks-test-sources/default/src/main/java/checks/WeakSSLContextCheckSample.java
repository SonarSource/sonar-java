package checks;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.net.ssl.SSLContext;
import okhttp3.ConnectionSpec;
import okhttp3.TlsVersion;

import static okhttp3.TlsVersion.SSL_3_0;
import static okhttp3.TlsVersion.TLS_1_1;

class WeakSSLContextCheckSample {

  private static final String PROTOCOL = "SSL";

  void foo(String protocol, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
    bar(SSLContext.getInstance(protocol));

    bar(SSLContext.getInstance("SSL")); // Noncompliant [[sc=32;ec=37]] {{Change this code to use a stronger protocol.}}
    bar(SSLContext.getInstance("SSLv2")); // Noncompliant
    bar(SSLContext.getInstance("SSLv3")); // Noncompliant
    bar(SSLContext.getInstance("TLS")); // Noncompliant
    bar(SSLContext.getInstance("TLSv1")); // Noncompliant
    bar(SSLContext.getInstance("TLSv1.1")); // Noncompliant
    bar(SSLContext.getInstance("TLSv1.2"));
    bar(SSLContext.getInstance("TLSv1.3"));
    bar(SSLContext.getInstance("DTLS")); // Noncompliant
    bar(SSLContext.getInstance("DTLSv1.0")); // Noncompliant
    bar(SSLContext.getInstance("DTLSv1.2"));
    bar(SSLContext.getInstance("DTLSv1.3"));

    bar(SSLContext.getInstance("SSL", provider)); // Noncompliant
    bar(SSLContext.getInstance("TLSv1.2", provider));
    bar(SSLContext.getInstance("TLSv1.2", "SSL"));

    bar(SSLContext.getInstance(PROTOCOL)); // Noncompliant
  }

  void bar(SSLContext ctx) {
    System.out.println(ctx);
  }

  void okHttp(String argumentVersion) {
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TlsVersion.TLS_1_0) // Noncompliant [[sc=20;ec=38]] {{Change this code to use a stronger protocol.}}
      .build();

    ConnectionSpec spec2 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TLS_1_1) // Noncompliant [[sc=20;ec=27]] {{Change this code to use a stronger protocol.}}
      .build();

    ConnectionSpec spec3 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TlsVersion.TLS_1_2) // Compliant
      .build();

    ConnectionSpec spec4 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TlsVersion.TLS_1_3) // Compliant
      .build();

    ConnectionSpec spec5 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(SSL_3_0) // Compliant
      .build();

    ConnectionSpec specWithString = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions("TLSv1") // Noncompliant [[sc=20;ec=27]]
      .build();

    ConnectionSpec specWithString2 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions("TLSv1.1") // Noncompliant
      .build();

    ConnectionSpec specWithString3 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions("TLSv1.2") // Compliant
      .build();

    ConnectionSpec specWithMultipleVersions = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_1) // Noncompliant [[sc=40;ec=58]]
      .build();

    ConnectionSpec specWithMultipleWeakVersions = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(TlsVersion.TLS_1_0, // Noncompliant [[sc=20;ec=38;secondary=+1]]
        TlsVersion.TLS_1_1)
      .build();

    ConnectionSpec specWithMultipleWeakVersions2 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions("TLSv1", // Noncompliant [[sc=20;ec=27;secondary=+1]]
        "TLSv1.1")
      .build();

    ConnectionSpec specWithUnknownValue = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(argumentVersion) // Compliant, unknown version
      .build();

    ConnectionSpec specWithUnknownValue2 = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
      .tlsVersions(getVersion()) // Compliant, unknown version
      .build();
  }

  String getVersion() {
    return "TLSv1.1";
  }

}
