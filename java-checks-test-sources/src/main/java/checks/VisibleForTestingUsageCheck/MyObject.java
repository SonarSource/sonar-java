package checks.VisibleForTestingUsageCheck;

import com.google.common.annotations.VisibleForTesting;

public class MyObject {

  MyObject() {}

  @VisibleForTesting
  MyObject(int i) {}

  @VisibleForTesting
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
