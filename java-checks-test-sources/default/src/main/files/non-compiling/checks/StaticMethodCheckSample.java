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
