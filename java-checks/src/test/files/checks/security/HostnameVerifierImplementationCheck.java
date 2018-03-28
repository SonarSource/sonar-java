import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

class A {
  private B b;

  A(B b) {
    this.b = b;
  }

  void method1() {
    return b.method(new HostnameVerifier() {
      @Override
      public boolean verify(String a, SSLSession b) {
        return true; // Noncompliant {{Do not unconditionally return true in this method.}}
      }
    });
  }

  void method2() {
    return b.method((a, b) -> true); // Noncompliant
  }

  void method3() {
    return b.method((a, b) -> (true)); // Noncompliant
  }

  void method4() {
    return b.method((a, b) -> {
      return true; // Noncompliant
    });
  }

  void method5() {
    return b.method((a, b) -> {
      return (true); // Noncompliant
    });
  }

  void method6() {
    new B((a, b) -> true); // Noncompliant
  }

  void method7() {
    new B((a, b) -> false);
  }

  void method8() {
    return b.method(new HostnameVerifier() {
      @Override
      public boolean verify(String a, SSLSession b) {
        return a.equalsIgnoreCase(b.getPeerHost());
      }
    });
  }

  void method9() {
    new B((a, b) -> a.equalsIgnoreCase(b.getPeerHost()));
  }
}

class B {
  private HostnameVerifier hostnameVerifier;

  B(HostnameVerifier hostnameVerifier) {
    this.hostnameVerifier = hostnameVerifier;
  }

  void method(HostnameVerifier hostnameVerifier) {
    this.hostnameVerifier = hostnameVerifier;
  }
}
