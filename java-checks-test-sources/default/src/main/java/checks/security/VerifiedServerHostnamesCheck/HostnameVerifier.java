package checks.security.VerifiedServerHostnamesCheck;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

class A {
  private B b;

  A(B b) {
    this.b = b;
  }

  void method1() {
    b.method(new HostnameVerifier() {
      @Override
      public boolean verify(String a, SSLSession b) {
        return true; // Noncompliant {{Enable server hostname verification on this SSL/TLS connection.}}
      }
    });
    b.method(new HostnameVerifier() {
      @Override
      public boolean verify(String a, SSLSession b) {
        return a.equalsIgnoreCase(b.getPeerHost());
      }
    });
    b.method(new HostnameVerifier() {
      @Override
      public boolean verify(String a, SSLSession b) {
        switch (1) {
          case 1:
            return true;
          case 2:
          default:
            return false;
        }
      }
    });
  }

  void method2() {
    b.method((a, b) -> true); // Noncompliant
    b.method((a, b) -> (((true)))); // Noncompliant
    b.method((a, b) -> a.equalsIgnoreCase(b.getPeerHost()));
  }

  void method4() {
    b.method((a, b) -> {
      return true; // Noncompliant
    });
    b.method((a, b) -> {
      return (((true))); // Noncompliant
    });
    b.method((a, b) -> {
      return false;
    });
    b.method((a, b) -> {
      boolean returnValue = true;
      return returnValue;
    });
  }
}

class B {
  void method(HostnameVerifier hostnameVerifier) { }
}

class C implements HostnameVerifier {
  public boolean verify(String a, SSLSession b) {
    return true; // Noncompliant
  }
}

class D implements HostnameVerifier {
  public boolean verify(String a, SSLSession b) {
    return a.equals("");
  }
}

class E implements HostnameVerifier {
  public boolean verify(String a, SSLSession b) {
    {
      {
        ;
        return true; // Noncompliant
      }
    }
  }
}

class F implements HostnameVerifier {
  public boolean verify(String a, SSLSession b) {
    {
      boolean bool = true;
    }
    {
      return true;
    }
  }
}

abstract class G implements HostnameVerifier {
  public abstract boolean verify(String a, SSLSession b);
}
