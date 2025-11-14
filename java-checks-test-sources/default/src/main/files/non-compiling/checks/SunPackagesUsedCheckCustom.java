import com.sun.imageio.plugins; // Compliant
import com.sun.jersey.api.client.ClientHandlerException; // Compliant
import java.util.ArrayList;

class SunPackagesUsedCheckCustom {
  private void f() {
    com.sun.imageio.plugins.bmp d =  // Compliant
      new com.sun.imageio.plugins.bmp(); // Compliant
    java.util.List a;
    sun.Foo b; // Compliant - without semantic info, we can't distinguish between package and variable
    db.setErrorHandler(new com.sun.org.apache.xml.internal.security.utils
        .IgnoreAllErrorHandler());
    sun       // Compliant
        .Foo.toto
        .asd c;

    sun.excluded.Foo foo = null; // Compliant, excluded

  }
}
