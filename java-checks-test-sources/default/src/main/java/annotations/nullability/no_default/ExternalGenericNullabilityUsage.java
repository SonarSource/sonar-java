package annotations.nullability.no_default;

import java.util.List;
import org.eclipse.jdt.annotation.NonNull;

public class ExternalGenericNullabilityUsage {

  public void calls(ExternalGenericNullability<@NonNull String> dependency) {
    dependency.declaredNonNull("value");
    dependency.declaredNullable(null);
    dependency.nestedNullable(List.of("value"));
    dependency.inherited(null);
    new ExternalGenericNullability<@NonNull String>(null);
    dependency.<@NonNull String>genericMethod("value");
    dependency.<@NonNull String>genericVarargs("value");
    dependency.defaulted("value");
  }
}
