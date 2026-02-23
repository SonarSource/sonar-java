package checks;

class ScopedValueStableReferenceCheckSample {

  private static final ScopedValue<String> VALUE = ScopedValue.newInstance();

  public void whereNC() {
    ScopedValue.where(ScopedValue.newInstance(), "inaccessible").run(() -> { // Noncompliant {{Consider using a stable reference for ScopedValue instances.}}
//                    ^^^^^^^^^^^^^^^^^^^^^^^^^
      // Cannot reference the scoped value here, as it has no name.
    });
  }

  public String readNewInstance() {
    // A new instance of a scoped value can never be bound without passing it as an argument to `where`.
    return ScopedValue.<String>newInstance().get(); // Noncompliant {{Consider using a stable reference for ScopedValue instances.}}
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  public boolean newInstanceIsBound() {
    // A new instance of a scoped value can never be bound without passing it as an argument to `where`.
    return ScopedValue.newInstance().isBound(); // Noncompliant {{Consider using a stable reference for ScopedValue instances.}}
//         ^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  public String readFieldInWhere() {
    return ScopedValue.where(VALUE, "field value").call(VALUE::get); // Compliant
  }

  public String readLocalVarInWhere() {
    ScopedValue<String> value = ScopedValue.newInstance();
    return ScopedValue.where(value, "local value").call(value::get); // Compliant
  }

  public String directRead() {
    return VALUE.get(); // Compliant
  }

}
