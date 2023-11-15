package annotations.nullability.no_default;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jdt.annotation.DefaultLocation;

public class NullabilityAtClassLevel {

  Object id3000_type_NO_ANNOTATION_level_PACKAGE;

  public Object id3001_type_NO_ANNOTATION_level_PACKAGE(
    Object id3002_type_NO_ANNOTATION_level_PACKAGE) {
    return new Object();
  }

}

@javax.annotation.ParametersAreNonnullByDefault
class NullabilityAtClassLevel2 {

  @javax.annotation.Nonnull
  @javax.annotation.Nonnull(when = When.MAYBE)
  Object id10472_type_STRONG_NULLABLE_level_VARIABLE_line_22;

  @javax.annotation.Nonnull(when = When.MAYBE)
  @javax.annotation.Nonnull
  Object id10473_type_STRONG_NULLABLE_level_VARIABLE_line_25;

  // line error, should be the second annotation instead of the first,
  // limitation to convert AnnotationInstance into AnnotationTree
  // we only check symbolType and number of values
  @javax.annotation.Nonnull(when = When.ALWAYS)
  @javax.annotation.Nonnull(when = When.MAYBE)
  Object id10472_type_STRONG_NULLABLE_level_VARIABLE_line_32;

  @javax.annotation.Nonnull(when = When.MAYBE)
  @javax.annotation.Nonnull(when = When.ALWAYS)
  Object id10473_type_STRONG_NULLABLE_level_VARIABLE_line_36;

}

@javax.annotation.ParametersAreNonnullByDefault
public class NullabilityAtClassLevel3 {

  @Unknown
  public Object id20011_type_UNKNOWN_level_METHOD(Object id20012_type_UNKNOWN_level_METHOD) {
    // Unknown annotation returns unknown nullability
    return new Object();
  }

}
