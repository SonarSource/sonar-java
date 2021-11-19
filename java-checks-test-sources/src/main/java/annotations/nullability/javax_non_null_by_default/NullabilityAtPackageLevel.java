package annotations.nullability.javax_non_null_by_default;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.eclipse.jdt.annotation.DefaultLocation;

public class NullabilityAtPackageLevel {

  // Only impacts parameters
  Object id4001_type_NO_ANNOTATION_level_PACKAGE;

  public Object id4002_type_NO_ANNOTATION_level_PACKAGE(
    Object id4003_type_NON_NULL_level_PACKAGE,
    // It is possible to override it
    @Nullable Object id4004_type_WEAK_NULLABLE_level_VARIABLE,
    @Nonnull Object id4005_type_NON_NULL_level_VARIABLE) {
    return new Object();
  }
}

@org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.PARAMETER)
class NullabilityAtPackageLevel2 {
  public Object id4006_type_NO_ANNOTATION_level_PACKAGE(
    Object id4007_type_NON_NULL_level_CLASS) {
    return new Object();
  }

}
