import com.sun.imageio.plugins;
import com.sun.jersey.api.client.ClientHandlerException;
import java.util.ArrayList;

class A {
  private void f() {
    com.sun.imageio.plugins.bmp d = new com.sun.imageio.plugins.bmp();
    new com.sun.imageio.plugins.bmp();
    java.util.List a;
    sun.Foo b; // Noncompliant {{Replace this usage of Sun classes by ones from the Java API.}}
    db.setErrorHandler(new com.sun.org.apache.xml.internal.security.utils
        .IgnoreAllErrorHandler());
    sun       // Noncompliant {{Replace this usage of Sun classes by ones from the Java API.}}
        .Foo.toto
        .asd c;

    new Foo<com.sun.Bar>() {}; // Noncompliant
  }
}
