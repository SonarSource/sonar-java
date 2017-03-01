import com.sun.imageio.plugins; // Noncompliant [[sc=8;ec=31;secondary=2,7,8,10,11,13,17]]  {{Use classes from the Java API instead of Sun classes.}}
import com.sun.jersey.api.client.ClientHandlerException; // secondary
import java.util.ArrayList;

class A {
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
