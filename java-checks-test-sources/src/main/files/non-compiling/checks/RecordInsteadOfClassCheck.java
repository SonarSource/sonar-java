package checks;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class RecordInsteadOfClassCheck {
  public final class NotCompiling extends Unknown { // Compliant because it inherits
    private final int i;

    public NotCompiling(final int i) {
      this.i = i;
    }

    public int getI() {
      return i;
    }
  }

  public final class DefinitelyFinal { // Noncompliant
    private final int i;

    public DefinitelyFinal(final int i) {
      this.i = i;
    }

    public int getI() {
      return i;
    }
  }
}
