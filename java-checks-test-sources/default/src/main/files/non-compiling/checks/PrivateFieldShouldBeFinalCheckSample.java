package checks;

class PrivateFieldShouldBeFinalCheckSample {

  // Flexible constructor bodies allow statements before the explicit this(...) invocation
  static class DelegatingAfterPrologue {

    private String value;

    DelegatingAfterPrologue(String value) {
      this.value = value;
    }

    DelegatingAfterPrologue() {
      String computed = "default";
      this(computed);
      this.value = "overwritten";
    }

    String read() {
      return value;
    }
  }

  static class DelegatingPrologueWithoutWrite {

    private String assignedOnlyInTarget; // Noncompliant

    DelegatingPrologueWithoutWrite(String value) {
      this.assignedOnlyInTarget = value;
    }

    DelegatingPrologueWithoutWrite() {
      String computed = "default";
      this(computed);
    }

    String read() {
      return assignedOnlyInTarget;
    }
  }
}
