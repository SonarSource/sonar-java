package checks;

class ScopedValueStableReferenceCheckSample {

  private static final ScopedValue<String> VALUE = ScopedValue.newInstance();

  public void whereNC() {
    ScopedValue.where(ScopedValue.newInstance(), "inaccessible").run(() -> { // Noncompliant {{Consider using a stable reference for ScopedValue instances.}}
//                    ^^^^^^^^^^^^^^^^^^^^^^^^^
      // Cannot reference the scoped value here, as it has no name.
    });
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
