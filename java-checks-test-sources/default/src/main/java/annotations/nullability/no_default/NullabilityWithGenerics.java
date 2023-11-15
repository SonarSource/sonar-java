package annotations.nullability.no_default;

import javax.annotation.Nullable;

public class NullabilityWithGenerics {

  public <T> void nullableArgument(@Nullable T... ts) {
  }

  public void callNullableArguments() {
    nullableArgument("a", "b");
    nullableArgument(1, 2);
  }
}
