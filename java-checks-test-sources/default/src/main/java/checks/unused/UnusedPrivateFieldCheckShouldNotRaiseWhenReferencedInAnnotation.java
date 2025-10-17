package checks.unused;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.FieldSources;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Target({METHOD, TYPE})
@Retention(RUNTIME)
@interface CustomAnnotation {
  String value();
}

@Target({METHOD})
@Retention(RUNTIME)
@interface CustomAnnotationWithDifferentLiteralType {
  int value();
}

public class UnusedPrivateFieldCheckShouldNotRaiseWhenReferencedInAnnotation {
  static class SONARJAVA5464Reproducer {
    // This is a regression test:
    // An FP used to be raised on the field below because we would not recognize that the field is referenced by a string in the
    // @FieldSource annotation below.
    private static final List<Arguments> multiArgs = List.of( // Compliant
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

  static class ShouldNotRaiseForExternalFieldsReferencedInAnnotation {
    // Accepted FP:
    // This field is referenced using a fully qualified name in the @FieldSource annotation below and thus it is not unused.
    // However, the additional logic we would need to track such fields with fully qualified names globally is not yet justified without
    // having seen some real world impact. Hence, we accept this FP for now.
    private static final List<Integer> externalField = List.of(1, 2, 3); // Noncompliant
    private static final List<Integer> unusedControlField = List.of(7, 8, 9); // Noncompliant {{Remove this unused "unusedControlField" private field.}}

    static class Nested {
      @ParameterizedTest
      @FieldSource("checks.unused.UnusedPrivateFieldCheckShouldNotRaiseWhenReferencedInAnnotation$ShouldNotRaiseForExternalFieldsReferencedInAnnotation#externalField")
      void test(int input) {
        // ...
      }
    }
  }

  static class ShouldNotRaiseForFieldsReferencedInNestedAnnotations {
    private static final List<Integer> firstField = List.of(1, 2, 3); // Compliant: Used in annotation below
    private static final List<Integer> secondField = List.of(4, 5, 6); // Compliant: Used in annotation below
    private static final List<Integer> unusedControlField = List.of(7, 8, 9); // Noncompliant {{Remove this unused "unusedControlField" private field.}}

    @ParameterizedTest
    @FieldSources({
      @FieldSource("firstField"),
      @FieldSource("secondField")
    })
    void test(int input) {
      // ...
    }
  }

  static class ShouldNotRaiseForFieldsReferencedInRepeatedAnnotations {
    private static final List<Integer> firstField = List.of(1, 2, 3); // Compliant: Used in annotation below
    private static final List<Integer> secondField = List.of(4, 5, 6); // Compliant: Used in annotation below
    private static final List<Integer> unusedControlField = List.of(7, 8, 9); // Noncompliant {{Remove this unused "unusedControlField" private field.}}

    @ParameterizedTest
    @FieldSource("firstField")
    @FieldSource("secondField")
    void test(int input) {
      // ...
    }
  }

  static class ShouldNotRaiseForShadowedFields {
    // This one is noncompliant, because the test in the nested class uses a different field that just has the same name
    private static final List<Integer> firstField = List.of(1, 2, 3); // Noncompliant {{Remove this unused "firstField" private field.}}
    private static final List<Integer> secondField = List.of(4, 5, 6); // Compliant: Used in an annotation within this class below

    static class Nested {
      private static final List<Integer> firstField = List.of(1, 2, 3); // Compliant: Used in annotation below
      private static final List<Integer> secondField = List.of(4, 5, 6); // Noncompliant {{Remove this unused "secondField" private field.}}

      @ParameterizedTest
      @FieldSource("firstField")
      void test(int input) {
        // ...
      }
    }

    @ParameterizedTest
    @FieldSource("secondField")
    void test(int input) {
      // ...
    }
  }

  static class ShouldRaiseWhenFieldIsIncorrectlyReferencedInNestedClass {
    // TP:
    // The FieldSource in the nested class is always referencing local fields, unless the name is fully qualified.
    // Hence, this field is indeed unused.
    private static final List<Integer> field = List.of(1, 2, 3); // Noncompliant {{Remove this unused "field" private field.}}

    @org.junit.jupiter.api.Nested
    class Nested {
      @ParameterizedTest
      @FieldSource("field")
      void test(int input) {
        // ...
      }
    }
  }

  static class ShouldNotRaiseWhenFieldIsReferencedByCustomAnnotation {
    // The following is a potential FP:
    // @CustomAnnotation might function similarly to @FieldSource, so we might not want to raise here.
    // However, if we mute the rule for unknown annotations, we lose TPs on ruling. Hence, we only support FieldSource and FieldSources atm.
    private static final List<Integer> field = List.of(1, 2, 3); // Noncompliant {{Remove this unused "field" private field.}}

    @ParameterizedTest
    @CustomAnnotation("field")
    void test(int input) {
      // ...
    }
  }

  static class ShouldRaiseWhenFieldIsReferencedByNonMethodAnnotation {
    // We only consider method annotations for now, hence, we intentionally raise for fields referenced in class annotations etc:
    private static final List<Integer> field = List.of(1, 2, 3); // Noncompliant {{Remove this unused "field" private field.}}

    @org.junit.jupiter.api.Nested
    @FieldSource("field")
    class Nested {
      @ParameterizedTest
      void test(int input) {
        // ...
      }
    }
  }
}
