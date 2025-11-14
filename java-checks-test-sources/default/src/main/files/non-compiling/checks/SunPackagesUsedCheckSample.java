import java.util.ArrayList;

class SunPackagesUsedCheckSample {
  private Object sun; // variable named "sun"

  private void f() {
    java.util.List a;
    sun.Foo b; // Compliant - without semantic info, we can't distinguish between package and variable
    sun.Foo.toto.asd c; // Compliant

  }

  public Object uselessMethod() {
    if (com.sun.xml.ws.developer.JAXWSProperties.CONNECT_TIMEOUT.equals("com.sun.xml.ws.connect.timeout")) { // compliant
      return new com.sun.xml.ws.transport.http.HttpAdapter(null, null, null); // compliant
    }
    return null;
  }

  // SONARJAVA-4698: False positive when variable is named "sun"
  public void fooWithFieldNamedSun() {
    sun.toString(); // Compliant - "sun" is a field of type Object, not a sun.* package class
  }

  public void barWithParameterNamedSun(Object sun) {
    sun.toString(); // Compliant - "sun" is a parameter of type Object, not a sun.* package class
  }

  public void bazWithLocalVariableNamedSun() {
    Object sun = new Object();
    sun.toString(); // Compliant - "sun" is a local variable of type Object, not a sun.* package class
  }
}
