package annotations.nullability.no_default;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jdt.annotation.DefaultLocation;

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
  @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.FIELD)
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
@org.eclipse.jdt.annotation.NonNullByDefault
class NullabilityAtClassLevel3 {

  Object id3012_type_NON_NULL_level_CLASS;

  public Object id3013_type_NON_NULL_level_CLASS(
    Object id3014_type_NON_NULL_level_CLASS) {
    return new Object();
  }
}

// Target only fields
@org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.FIELD)
class NullabilityAtClassLevel4 {

  Object id3015_type_NON_NULL_level_CLASS;

  public Object id3016_type_NO_ANNOTATION_level_PACKAGE(
    Object id3017_type_NO_ANNOTATION_level_PACKAGE) {
    return new Object();
  }
}

// Target only parameters
@org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.PARAMETER)
class NullabilityAtClassLevel5 {

  Object id3018_type_NO_ANNOTATION_level_PACKAGE;

  public Object id3019_type_NO_ANNOTATION_level_PACKAGE(
    Object id3020_type_NON_NULL_level_CLASS) {
    return new Object();
  }
}

// Target return values
@org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.RETURN_TYPE)
class NullabilityAtClassLevel6 {

  Object id3021_type_NO_ANNOTATION_level_PACKAGE;

  public Object id3022_type_NON_NULL_level_CLASS(
    Object id3023_type_NO_ANNOTATION_level_PACKAGE) {
    return new Object();
  }
}

// Target something else
@org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.ARRAY_CONTENTS)
class NullabilityAtClassLevel7 {

  Object id3024_type_NO_ANNOTATION_level_PACKAGE;

  public Object id3025_type_NO_ANNOTATION_level_PACKAGE(
    Object id3026_type_NO_ANNOTATION_level_PACKAGE) {
    return new Object();
  }
}

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

