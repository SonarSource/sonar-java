class VisibleForTestingUsageCheck {
  @com.google.common.annotations.VisibleForTesting
  void bar() { }

  void foo() {
    unknown();
  }
}

class VisibleForTestingUsageCheck2 {
  void qix() {
    new VisibleForTestingUsageCheck()
      .bar(); // Compliant, in the same file
  }
}
