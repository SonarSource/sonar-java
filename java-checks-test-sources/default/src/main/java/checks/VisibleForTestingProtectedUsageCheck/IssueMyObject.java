package checks.VisibleForTestingProtectedUsageCheck;

public class IssueMyObject {

  IssueMyObject() {
  }

  // androidx.annotation.VisibleForTesting
  // @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  // where VisibleForTesting.PROTECTED = 4
  @VisibleForTesting(otherwise = 3, othertestcase=1)
  String foo;

}
