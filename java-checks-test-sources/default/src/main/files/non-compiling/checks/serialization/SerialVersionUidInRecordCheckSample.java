package checks.serialization;

import java.io.Serial;
import java.io.Serializable;

import org.sonar.somepackage.SomeUnknownClass;

public class SerialVersionUidInRecordCheckSample {

  record NotInitialized(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID; // Compliant
  }

  record InitalizedWithUninitializedLong(String name, int age) implements Serializable {
    static final long UNINITIALIZED_LONG;
    @Serial private static final long serialVersionUID = UNINITIALIZED_LONG; // Compliant
  }

  record InitalizedWithUninitializedInteger(String name, int age) implements Serializable {
    static final int UNINITIALIZED_INTEGER;
    @Serial private static final long serialVersionUID = UNINITIALIZED_INTEGER; // Compliant
  }

  record InitializedWithUnknowConstant(String name, int age) implements Serializable {
    @Serial private static final long serialVersionUID = SomeUnknownClass.UNKNOWN_LONG_VALUE; // Compliant
  }
}
