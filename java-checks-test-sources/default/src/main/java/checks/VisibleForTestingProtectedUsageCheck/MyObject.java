package checks.VisibleForTestingProtectedUsageCheck;

public class MyObject {

  MyObject() {}

  // androidx.annotation.VisibleForTesting
  // @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  // where VisibleForTesting.PROTECTED = 4
  @VisibleForTesting(otherwise = 4, othertestcase=1, othertypecase="F")
  String foo;

  @VisibleForTesting(otherwise = 4, othertestcase=1, othertypecase="F")
  int answer() {
    return 42;
  }

  int answer(int result) {
    return result;
  }

  @VisibleForTesting(otherwise = 4, othertestcase=1, othertypecase="F")
  class Nested {}
}

@VisibleForTesting(otherwise = 4, othertestcase=1, othertypecase="F")
class Outer {}



