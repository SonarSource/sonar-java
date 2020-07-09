package checks.UselessImportCheck;

import java.io.Serializable; // Compliant
import java.util.Optional; // Noncompliant
import java.util.function.Function;

class TestingSerializable {
  Function<String, Integer> getScalarReferenceResolver() {
    return (Function<String, Integer> & Serializable) absoluteReference -> {
      return null;
    };
  }
}
