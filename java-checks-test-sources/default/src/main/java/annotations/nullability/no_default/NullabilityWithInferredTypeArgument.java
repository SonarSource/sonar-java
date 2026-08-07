package annotations.nullability.no_default;

import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;

public class NullabilityWithInferredTypeArgument {

  private final String value = "value";

  @NonNull
  public String getValue() {
    return value;
  }

  public String callOrElse(NullabilityWithInferredTypeArgument a) {
    // "map" infers "Optional<@NonNull String>", the @NonNull is part of the type argument and says nothing about the parameter of "orElse"
    return Optional.ofNullable(a)
      .map(NullabilityWithInferredTypeArgument::getValue)
      .orElse(null);
  }

  public boolean nullCheckAfterOrElse(NullabilityWithInferredTypeArgument a) {
    String result = Optional.ofNullable(a)
      .map(NullabilityWithInferredTypeArgument::getValue)
      .orElse(null);
    // the inferred @NonNull must not make "orElse" look like it never returns null, the check below is not always false
    return result == null;
  }
}
