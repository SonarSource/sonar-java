package checks.serialization;

import java.io.Serializable;
import java.util.Map;

class Address {
}
class Person implements Serializable {
  Address address; // Noncompliant [[sc=11;ec=18]] {{Make "address" transient or serializable.}}
  UnknownField unknownField; // Compliant
}

class Person7 implements Serializable {
  private Map<String, String> ok; // Compliant

  void foo() {
    ok = unknown(); // Compliant
  }
}
