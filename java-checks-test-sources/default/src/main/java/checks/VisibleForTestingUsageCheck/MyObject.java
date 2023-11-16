package checks.VisibleForTestingUsageCheck;

public class MyObject {

  MyObject() {}

  @com.google.common.annotations.VisibleForTesting
  MyObject(int i) {}

  @checks.VisibleForTestingUsageCheck.VisibleForTesting
  String foo;

  @VisibleForTesting
  int answer() {
    return 42;
  }

  int answer(int result) {
    return result;
  }

  @VisibleForTesting
  class Nested {}
}

@VisibleForTesting
class Outer {}



