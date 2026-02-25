package checks;

import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;

class ScopedValueStableReferenceCheckSample {

  private static final ScopedValue<String> VALUE = ScopedValue.newInstance();

  public void where() {
    ScopedValue.where(ScopedValue.newInstance(), "inaccessible").run(() -> { // Noncompliant {{Consider using a stable reference for ScopedValue instances.}}
//                    ^^^^^^^^^^^^^^^^^^^^^^^^^
      // Cannot reference the scoped value here, as it has no name.
    });
  }

  public void chainedWhere() {
    ScopedValue<String> scopedValue = ScopedValue.newInstance();
    ScopedValue.where(scopedValue, "accessible").where(ScopedValue.newInstance(), "inaccessible").run(() -> { // Noncompliant {{Consider using a stable reference for ScopedValue instances.}}
//                                                     ^^^^^^^^^^^^^^^^^^^^^^^^^
    });
  }

  public void nestedArgument() {
    ScopedValue.where(Objects.requireNonNull(ScopedValue.newInstance()), "scopedValue").run(() -> {}); // Noncompliant {{Consider using a stable reference for ScopedValue instances.}}
//                                           ^^^^^^^^^^^^^^^^^^^^^^^^^
    ScopedValue.where((ScopedValue.newInstance()), "scopedValue").run(() -> {}); // Noncompliant {{Consider using a stable reference for ScopedValue instances.}}
//                     ^^^^^^^^^^^^^^^^^^^^^^^^^
    ScopedValue.where(Pair.of(ScopedValue.newInstance(), 0).getLeft(), "scopedValue").run(() -> {}); // Noncompliant {{Consider using a stable reference for ScopedValue instances.}}
//                            ^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  public String readFieldInWhere() {
    return ScopedValue.where(VALUE, "field value").call(VALUE::get); // Compliant
  }

  public String readLocalVarInWhere() {
    ScopedValue<String> value = ScopedValue.newInstance();
    return ScopedValue.where(value, "local value").call(value::get); // Compliant
  }

}
