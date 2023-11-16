package symbolicexecution.checks;

import java.util.List;
import javax.annotation.Nullable;

public abstract class NullDereferenceCheck_constants_in_loop {

  void foo(List<Object> items) {
    Integer max = null;
    for (Object item : items) {
      Integer value = getValue(item);
      if (value == null) {
        value = Integer.MAX_VALUE; // Should be a symbolic value from ConstraintManager#constants
      }
      if (max == null) {
        max = value;
      } else if (value == max) {
        max = value;
      } else if (value.equals(max)) { // Noncompliant {{A "NullPointerException" could be thrown; "value" is nullable here.}}
        doSomething();
      }
    }
  }

  void doSomething() { }

  @Nullable
  abstract Integer getValue(Object o);

}
