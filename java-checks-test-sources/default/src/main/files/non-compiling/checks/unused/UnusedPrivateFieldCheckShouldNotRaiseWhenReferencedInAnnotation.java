package checks.unused;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import static org.assertj.core.api.Assertions.assertThat;

public class UnusedPrivateFieldCheckShouldNotRaiseWhenReferencedInAnnotation {
  static class ShouldNotRaiseForDuplicateFields {
    private static final List<Integer> field = List.of(1, 2, 3); // Compliant: Used in annotation below
    private static final List<Integer> field = List.of(4, 5, 6); // Compliant: Used in annotation below
    private static final List<Integer> unusedControlField = List.of(7, 8, 9); // Noncompliant {{Remove this unused "unusedControlField" private field.}}

    @ParameterizedTest
    @FieldSource("field")
    void test(int input) {
      assertThat(input).isGreaterThan(0);
    }
  }
}
