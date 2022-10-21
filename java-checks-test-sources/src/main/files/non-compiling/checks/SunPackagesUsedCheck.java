import com.sun.imageio.plugins; // Noncompliant [[sc=8;ec=31;secondary=2,11,12,14,15,17,21]]  {{Use classes from the Java API instead of Sun classes.}}
import com.sun.security.ntlm.Client; // secondary
import com.sun.jersey.api.client.ClientHandlerException; // com.sun.jersey is excluded by default since it has nothing to do with Java interal
import com.sun.faces.application.ApplicationAssociate; // Excluded by default
import com.sun.xml.ws.developer.JAXWSProperties; // Excluded by default because not part of the JDK
import com.sun.xml.ws.transport.http.HttpAdapter; // Excluded by default because not part of the JDK
import java.util.ArrayList;

class SunPackagesUsedCheck {
  private void f() {
    com.sun.imageio.plugins.bmp d = // secondary
      new com.sun.imageio.plugins.bmp(); // secondary
    java.util.List a;
    sun.Foo b; // secondary
    db.setErrorHandler(new com.sun.org.apache.xml.internal.security.utils // secondary
        .IgnoreAllErrorHandler());
    sun       // secondary
        .Foo.toto
        .asd c;

    new Foo<com.sun.Bar>() {}; // secondary
  }

  public Object uselessMethod() {
    if (com.sun.xml.ws.developer.JAXWSProperties.CONNECT_TIMEOUT.equals("com.sun.xml.ws.connect.timeout")) { // compliant
      return new com.sun.xml.ws.transport.http.HttpAdapter(null, null, null); // compliant
    }
    return null;
  }
}
