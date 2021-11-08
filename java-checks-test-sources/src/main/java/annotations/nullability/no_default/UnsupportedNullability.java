package annotations.nullability.no_default;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class UnsupportedNullability {

  public Object id1001_type_UNKNOWN(
    Object id1002_type_NON_NULL_level_CLASS_line_9
  ) {
    // @ParametersAreNonnullByDefault does not impact lambda parameters
    Predicate<String> lambda1 = id1003_type_UNKNOWN -> false;
    // and the direct annotations are not yet supported on lambda parameters
    Predicate<String> lambda2 = (@Nonnull String id1004_type_UNKNOWN) -> false;

    // @ParametersAreNonnullByDefault does not impact local variables
    Object id1005_type_UNKNOWN = new Object();
    try (InputStream id1006_type_UNKNOWN = null) {

    } catch (IOException id1007_type_UNKNOWN) {

    }
    // and the direct annotations are not yet supported on local variables
    @Nonnull
    Object id1008_type_UNKNOWN = new Object();
    return id1008_type_UNKNOWN;
  }

}
