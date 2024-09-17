package checks.VisibleForTestingProtectedUsageCheck;

public class IssueMyObject {

  IssueMyObject() {
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED, othertestcase=1, othertypecase="S")
  String bar;

  // androidx.annotation.VisibleForTesting
  // @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  // where VisibleForTesting.PROTECTED = 4
  @VisibleForTesting(otherwise = 3, othertestcase=1, othertypecase="F")
  String foo;

}
