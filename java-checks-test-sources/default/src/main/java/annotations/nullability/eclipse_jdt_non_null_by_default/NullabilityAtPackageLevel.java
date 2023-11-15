package annotations.nullability.eclipse_jdt_non_null_by_default;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Package is annotated with "@org.eclipse.jdt.annotation.NonNullByDefault", targeting methods, fields and parameters
 */
public class NullabilityAtPackageLevel {

  Object id5001_type_NON_NULL_level_PACKAGE_line_empty;
  @Nonnull Object id5002_type_NON_NULL_level_VARIABLE_line_12;

  public Object id5003_type_NON_NULL_level_PACKAGE(
    Object id5004_type_NON_NULL_level_PACKAGE,
    // It is possible to override it
    @Nullable Object id5005_type_WEAK_NULLABLE_level_VARIABLE,
    @Nonnull Object id5006_type_NON_NULL_level_VARIABLE) {
    return new Object();
  }
}

@org.eclipse.jdt.annotation.NonNullByDefault
class NullabilityAtPackageLevel2 {
  public Object id5007_type_NON_NULL_level_CLASS_line_23(
    Object id5008_type_NON_NULL_level_CLASS_line_23) {
    return new Object();
  }

}
