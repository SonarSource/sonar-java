package symbolicexecution.checks;

import java.util.Optional;
import javax.annotation.CheckForNull;

record OptionalOfNullableCall_javax(@CheckForNull String attribute) {

  public Optional<String> foo() {
    return Optional.ofNullable(attribute).filter("foo"::equals);
  }

}
