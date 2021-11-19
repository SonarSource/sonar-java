package annotations.nullability.spring_non_null_fields;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jdt.annotation.DefaultLocation;

/**
 * Package is annotated with "org.springframework.lang.NonNullFields" targeting fields only
 */
public class NullabilityAtPackageLevel {

  Object id4001_type_NON_NULL_level_PACKAGE;
  @Nonnull Object id4002_type_NON_NULL_level_VARIABLE;
  @Nullable Object id4003_type_WEAK_NULLABLE_level_VARIABLE;

  public Object id4004_type_NO_ANNOTATION_level_PACKAGE(
    Object id4005_type_NO_ANNOTATION_level_PACKAGE) {
    return new Object();
  }
}

@org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.FIELD)
class NullabilityAtPackageLevel2 {
  Object id4006_type_NON_NULL_level_CLASS;
}
