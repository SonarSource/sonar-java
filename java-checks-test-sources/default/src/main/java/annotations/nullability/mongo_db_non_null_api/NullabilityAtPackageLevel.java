package annotations.nullability.mongo_db_non_null_api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Package is annotated with "@com.mongodb.lang.NonNullApi", targeting methods and parameters
 */
public class NullabilityAtPackageLevel {

  Object id5001_type_NO_ANNOTATION_level_PACKAGE;
  @Nonnull Object id5002_type_NON_NULL_level_VARIABLE;

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
  public Object id5007_type_NON_NULL_level_CLASS(
    Object id5008_type_NON_NULL_level_CLASS) {
    return new Object();
  }

}
