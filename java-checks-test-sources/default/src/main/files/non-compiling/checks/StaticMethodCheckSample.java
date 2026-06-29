package checks;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

class Utilities {
  private static String magicWord = "magic";
  private static String string = magicWord; // coverage
  private String otherWord = "other";

  public Utilities() {
  }

  private void registerPrimitives(final boolean type) {
    register(Boolean.TYPE, new Toto());
  }

  private Unknown unknown() { // Compliant because we should not make any decision on an unknown class
    return new Unknown("", "");
  }
  
}

class UtilitiesExtension extends Utilities {
  public UtilitiesExtension() {
  }
  private void method() { // Compliant
    publicMethod();
  }
}

class SerializableExclusions implements Serializable {
  private Object writeReplace() throws ObjectStreamException { }
}

class LifecycleAnnotations {
  private static int counter = 0;

  @javax.annotation.PostConstruct
  private void initPostConstruct() { // Compliant - lifecycle method
    System.out.println("PostConstruct");
  }

  @jakarta.annotation.PreDestroy
  private void cleanupPreDestroy() { // Compliant - lifecycle method
    System.out.println("PreDestroy");
  }

  @javax.ejb.PostActivate
  private void activatePostActivate() { // Compliant - lifecycle method
    System.out.println("PostActivate");
  }

  @jakarta.ejb.PrePassivate
  private void passivatePrePassivate() { // Compliant - lifecycle method
    System.out.println("PrePassivate");
  }

  @io.quarkus.runtime.Startup
  private void startupMethod() { // Compliant - Quarkus startup method
    System.out.println("Startup");
  }

  private void regularMethod() { // Noncompliant {{Make "regularMethod" a "static" method.}}
    counter++;
  }
}
