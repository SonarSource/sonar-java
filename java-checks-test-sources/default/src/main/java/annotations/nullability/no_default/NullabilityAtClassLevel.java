package annotations.nullability.no_default;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NullabilityAtClassLevel {

  Object id3000_type_NO_ANNOTATION_level_PACKAGE_line_empty;

  public Object id3001_type_NO_ANNOTATION_level_PACKAGE(
    Object id3002_type_NO_ANNOTATION_level_PACKAGE) {
    return new Object();
  }

}

@javax.annotation.ParametersAreNonnullByDefault
class NullabilityAtClassLevel2 {

  // No effect on fields
  Object id3003_type_NO_ANNOTATION_level_PACKAGE;

  // No effects on methods return value
  public Object id3004_type_NO_ANNOTATION_level_PACKAGE(
    Object id3005_type_NON_NULL_level_CLASS) {
    return new Object();
  }

  public Object id3006_type_NO_ANNOTATION_level_PACKAGE(
    // Variable level has priority
    @Nullable Object id3007_type_WEAK_NULLABLE_level_VARIABLE) {
    return new Object();
  }

  // Annotations targetting something else should not change the value
  public Object id3008_type_NO_ANNOTATION_level_PACKAGE(
    Object id3009_type_NON_NULL_level_CLASS) {
    return new Object();
  }

  @javax.annotation.ParametersAreNonnullByDefault
  public Object id3010_type_NO_ANNOTATION_level_PACKAGE(
    // level is equal to the first annotation in the hierarchy
    Object id3011_type_NON_NULL_level_METHOD) {
    return new Object();
  }

}

// Without arguments, everything is targeted


// Target only fields


// Target only parameters


// Target return values


// Target something else


@javax.annotation.ParametersAreNullableByDefault
class NullabilityAtClassLevel8 {
  // No effect on fields
  Object id3026_type_NO_ANNOTATION_level_PACKAGE;

  // No effects on methods return value
  public Object id3027_type_NO_ANNOTATION_level_PACKAGE(
    Object id3028_type_WEAK_NULLABLE_level_CLASS) {
    return new Object();
  }

  public Object id3029_type_NO_ANNOTATION_level_PACKAGE(
    // Variable level has priority
    @Nonnull Object id3030_type_NON_NULL_level_VARIABLE) {
    return new Object();
  }
}

@javax.annotation.ParametersAreNullableByDefault
abstract class NullabilityAtClassLevel9 {
  abstract Object id3031_type_NO_ANNOTATION_level_PACKAGE(Object id3032_type_WEAK_NULLABLE_level_CLASS);

  class NestedClass {
    Object id3033_type_NO_ANNOTATION_level_PACKAGE(
      Object id3034_type_WEAK_NULLABLE_level_CLASS_line_130 // Annotation of nested class takes the annotation from the enclosing one.
    ) {
      return new NullabilityAtClassLevel9() {
        @Override
        Object id3031_type_NO_ANNOTATION_level_PACKAGE(
          Object id30322_type_WEAK_NULLABLE_level_CLASS_line_130
        ) {
          return null;
        }
      };
    }
  }
}

