package checks;

record MissingDeprecatedCheckWithRecords(@Deprecated int lo, int hi) {
  public MissingDeprecatedCheckWithRecords {
  @Deprecated
    int x = 42;
  }

  @Deprecated
  void foo() {} // Noncompliant
}

// This is a dangling JavaDoc, which is not supported, so we do not require annotations.
record Person(
  String name,
  /**
   * @deprecated
   */
  String address  // Compliant
) {}
