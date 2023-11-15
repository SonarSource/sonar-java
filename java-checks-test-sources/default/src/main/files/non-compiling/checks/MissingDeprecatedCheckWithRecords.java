package checks;

record MissingDeprecatedCheckWithRecords(@Deprecated int lo, int hi) { // Noncompliant
  public MissingDeprecatedCheckWithRecords {
  @Deprecated
    int x = 42;
  }

  @Deprecated
  void foo() {}  // Noncompliant
}
