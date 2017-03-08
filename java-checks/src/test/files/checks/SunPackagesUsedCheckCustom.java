import com.sun.imageio.plugins; // Compliant
import com.sun.jersey.api.client.ClientHandlerException; // Compliant
import java.util.ArrayList;

class A {
  private void f() {
    com.sun.imageio.plugins.bmp d =  // Compliant
      new com.sun.imageio.plugins.bmp(); // Compliant
    java.util.List a;
    sun.Foo b; // Noncompliant [[sc=5;ec=12;secondary=13,17]]  {{Use classes from the Java API instead of Sun classes.}}
    db.setErrorHandler(new com.sun.org.apache.xml.internal.security.utils
        .IgnoreAllErrorHandler());
    sun       // secondary
        .Foo.toto
        .asd c;

    new Foo<com.sun.Bar>() {}; // secondary
  }
}
