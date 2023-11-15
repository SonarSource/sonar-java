package annotations.nullability.no_default;

import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class UnsupportedNullability {

  public Object id1001_type_NO_ANNOTATION_level_PACKAGE(
    Object id1002_type_NON_NULL_level_CLASS_line_7
  ) {
    // @ParametersAreNonnullByDefault does not impact lambda parameters
    Predicate<String> lambda1 = id1003_type_UNKNOWN_level_UNKNOWN -> false;
    // and the direct annotations are not yet supported on lambda parameters
    Predicate<String> lambda2 = (@Nonnull String id1004_type_UNKNOWN_level_UNKNOWN) -> false;

    return new Object();
  }

}
