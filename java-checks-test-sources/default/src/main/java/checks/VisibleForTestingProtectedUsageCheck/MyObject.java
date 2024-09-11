package checks.VisibleForTestingProtectedUsageCheck;

public class MyObject {

  MyObject() {}

  // androidx.annotation.VisibleForTesting
  // @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  // where VisibleForTesting.PROTECTED = 4
  @VisibleForTesting(otherwise = 4, othertestcase=1)
  String foo;

  @VisibleForTesting(otherwise = 4, othertestcase=1)
  int answer() {
    return 42;
  }

  int answer(int result) {
    return result;
  }

  @VisibleForTesting(otherwise = 4, othertestcase=1)
  class Nested {}
}

@VisibleForTesting(otherwise = 4, othertestcase=1)
class Outer {}



