package symbolicexecution.behaviorcache;

import java.util.Optional;
import javax.annotation.Nonnull;

public class AnonymousExceptionalYield {
  static void foo(@Nonnull Optional<String> o) {
    o.orElseThrow(() -> new RuntimeException() { /* anonymous class */ });
  }
}
