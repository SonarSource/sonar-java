import com.sun.imageio.plugins; // Noncompliant [[sc=8;ec=31;secondary=2,9,10,12,13,15,19]]  {{Use classes from the Java API instead of Sun classes.}}
import com.sun.security.ntlm.Client; // secondary
import com.sun.jersey.api.client.ClientHandlerException; // com.sun.jersey is excluded by default since it has nothing to do with Java interal
import com.sun.faces.application.ApplicationAssociate; // Excluded by default
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
}
