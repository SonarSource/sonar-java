import java.util.ArrayList;

class SunPackagesUsedCheck {
  private void f() {
    java.util.List a;
    sun.Foo b; // Noncompliant [[secondary=+1]]
    sun.Foo.toto.asd c; // secondary
    
  }

  public Object uselessMethod() {
    if (com.sun.xml.ws.developer.JAXWSProperties.CONNECT_TIMEOUT.equals("com.sun.xml.ws.connect.timeout")) { // compliant
      return new com.sun.xml.ws.transport.http.HttpAdapter(null, null, null); // compliant
    }
    return null;
  }
}
