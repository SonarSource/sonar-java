import java.util.Objects;

class HashCodeMismatchedFieldsCheckSampleNonCompiling {

  // "undefinedField" cannot be resolved: the field-read collector must bail out on the unknown
  // identifier symbol instead of assuming it is unrelated state, and must not report an issue here.
  static class UnresolvedIdentifier {
    private final long id;

    UnresolvedIdentifier(long id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof UnresolvedIdentifier that && id == that.id;
    }

    @Override
    public int hashCode() { // No issue - "undefinedField" cannot be resolved, so the scan bails out
      return Objects.hash(id, undefinedField);
    }
  }

  // A call to an unresolvable method must also make the scan bail out rather than treat it as a
  // harmless side-effect-free helper.
  static class UnresolvedHelperCall {
    private final long id;
    private final int version;

    UnresolvedHelperCall(long id, int version) {
      this.id = id;
      this.version = version;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof UnresolvedHelperCall that && id == that.id && unresolvedHelper();
    }

    @Override
    public int hashCode() { // No issue - equals() calls an unresolved method, so the scan bails out
      return Objects.hash(id, version);
    }
  }
}
