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
        return true; // Noncompliant {{Do not unconditionally return true in this method.}}
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
    BiFunction<String, SSLSession, Boolean> lambda = (a, b) -> true;
    b.method((a, b) -> true); // Noncompliant
    b.method((a, b) -> (((true)))); // Noncompliant
    b.method((a, b) -> a.equalsIgnoreCase(b.getPeerHost()));
    b.method(lambda);
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
  void method(HostnameVerifier hostnameVerifier) {
    this.hostnameVerifier = hostnameVerifier;
  }
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

class G implements HostnameVerifier {
  public abstract boolean verify(String a, SSLSession b);
}
