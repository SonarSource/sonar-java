package checks.unused;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class UnusedPrivateFieldCheckShouldNotRaiseWhenReferencedInAnnotation {
  static class SONARJAVA5464Reproducer {
    private static final List<Arguments> multiArgs = List.of( // FP
      arguments(3, 6),
      arguments(5, 10),
      arguments(7, 14));

    Multiplication multiplication = new Multiplication();

    @ParameterizedTest
    @FieldSource("multiArgs")
    void testTimesTwo(Integer input, Integer expected) {
      assertThat(multiplication.timesTwo(input)).isEqualTo(expected);
    }

    static class Multiplication {
      public int timesTwo(int arg) {
        return arg * 2;
      }
    }
  }

  static class ShouldNotRaiseForMultipleFieldsReferencedInSameAnnotation {
    private static final List<Integer> firstField = List.of(1, 2, 3); // Compliant: Used in annotation below
    private static final List<Integer> secondField = List.of(4, 5, 6); // Compliant: Used in annotation below
    private static final List<Integer> unusedControlField = List.of(7, 8, 9); // Noncompliant {{Remove this unused "unusedControlField" private field.}}

    @ParameterizedTest
    @FieldSource({"firstField", "secondField"})
    void test(int input) {
      // ...
    }
  }

  static class ShouldNotRaiseForFieldsThatMatchTestName {
    private static final List<Integer> test = List.of(1, 2, 3); // Compliant: Has same name as the test below
    private static final List<Integer> unusedControlField = List.of(7, 8, 9); // Noncompliant {{Remove this unused "unusedControlField" private field.}}

    @ParameterizedTest
    @FieldSource
    void test(int input) {
      // ...
    }
  }
}
