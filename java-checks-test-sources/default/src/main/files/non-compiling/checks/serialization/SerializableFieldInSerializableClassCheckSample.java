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

class Person777 implements Serializable {
  Address address; // Compliant
  Address address1; // Compliant

  @Inject
  public Person777(Address _address, Address _address1) {
    int i = 0;
    address = _address;
    address1 = _address1;
    i = 5;
  }

  @Inject
  public Person777(Address _address) {
    int i = 0;
    address = _address;
    i = 5;
  }
}

class JakartaPerson777 implements Serializable {
  Address address; // Compliant
  Address address1; // Compliant

  @jakarta.inject.Inject
  public JakartaPerson777(Address _address, Address _address1) {
    int i = 0;
    address = _address;
    address1 = _address1;
    i = 5;
  }

  @jakarta.inject.Inject
  public JakartaPerson777(Address _address) {
    int i = 0;
    address = _address;
    i = 5;
  }
}
