import com.sun.imageio.plugins; // Non-Compliant
import com.sun.jersey.api.client.ClientHandlerException; // Non-Compliant
import java.util.ArrayList; // Compliant

class A {
  private void f() {
    com.sun.imageio.plugins.bmp a = new com.sun.imageio.plugins.bmp(); // Non-Compliant
    new com.sun.imageio.plugins.bmp(); // Non-Compliant
    java.util.List a; // Compliant
    sun.Foo a; // Non-Compliant
  }
}
