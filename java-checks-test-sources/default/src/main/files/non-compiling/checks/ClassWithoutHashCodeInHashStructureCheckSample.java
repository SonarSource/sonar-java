package checks;

import java.util.Hashtable;

class ClassWithoutHashCodeInHashStructureCheckSample {

  class E {
    Hashtable<String, Unknown> entries;
    E() { entries = new Hashtable<>(); } // Compliant - unknown type for values
  }
}
