package checks;

import java.util.Hashtable;

class ClassWithoutHashCodeInHashStructureCheck {

  class E {
    Hashtable<String, Unknown> entries;
    E() { entries = new Hashtable<>(); } // Compliant - unknown type for values
  }
}
