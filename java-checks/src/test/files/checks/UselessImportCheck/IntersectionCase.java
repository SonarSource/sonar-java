
package checks.UselessImportCheck;

import java.io.Serializable; // Compliant
import java.util.Optional;
import java.util.function.Function;

public class TestingSerializable {
    public static void main(String[] args) {
        Function<String, Optional<Integer>> a = TestingSerializable.getScalarReferenceResolver();
    }

    public static Function<String, Optional<Integer>> getScalarReferenceResolver() {
        return (Function<String, Optional<Integer>> & Serializable) absoluteReference -> {
            return null;
        };
    }
}
