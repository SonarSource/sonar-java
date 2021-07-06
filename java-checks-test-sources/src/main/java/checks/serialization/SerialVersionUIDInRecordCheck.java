package checks.serialization;

import java.io.Serial;
import java.io.Serializable;

public class SerialVersionUIDInRecordCheck {
  record Person(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID = 0L; // Noncompliant {{Remove this redundant "serialVersionUID" field}}
  }

  record Individual(String name, int age) implements Serializable {
  } // Compliant

  record Human(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID = 42L; // Compliant
  }
}
