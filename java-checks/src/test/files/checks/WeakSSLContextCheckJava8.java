import javax.net.ssl.SSLContext;

class A {

  void foo(String protocol, String provider) {
    bar(SSLContext.getInstance());

    bar(SSLContext.getInstance(protocol));

    bar(SSLContext.getInstance("SSL")); // Noncompliant
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
