package annotations.nullability.javax_nullable_by_default;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NullabilityAtPackageLevel {

  // Only impacts parameters
  Object id4001_type_NO_ANNOTATION_level_PACKAGE;

  public Object id4002_type_NO_ANNOTATION_level_PACKAGE(
    Object id4003_type_WEAK_NULLABLE_level_PACKAGE,
    // It is possible to override it
    @Nullable Object id4004_type_WEAK_NULLABLE_level_VARIABLE,
    @Nonnull Object id4005_type_NON_NULL_level_VARIABLE) {
    return new Object();
  }
}
