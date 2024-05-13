package checks;

import java.io.Serializable;

class TransientFieldInNonSerializableCheckSample {

  class A implements Serializable {
    transient String x;
    String y;
  }

  class B {
    transient String x; // Noncompliant {{Remove the "transient" modifier from this field.}}
//  ^^^^^^^^^
    String y;
    void myMethod() {}
  }

}
