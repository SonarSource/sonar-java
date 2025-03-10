package symbolicexecution.checks;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

record OptionalOfNullableCall_jspecify(@Nullable String attribute) {

  public Optional<String> foo() {
    return Optional.ofNullable(attribute).filter("foo"::equals);
  }

}
