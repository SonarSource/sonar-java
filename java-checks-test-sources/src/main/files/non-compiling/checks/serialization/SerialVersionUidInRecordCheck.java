package checks.serialization;

import java.io.Serial;
import java.io.Serializable;

public class SerialVersionUidInRecordCheck {
  record Human(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID; // Compliant
  }
}
