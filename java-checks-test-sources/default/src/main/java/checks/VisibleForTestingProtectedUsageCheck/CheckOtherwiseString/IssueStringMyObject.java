package checks.VisibleForTestingProtectedUsageCheck.CheckOtherwiseString;

public class IssueStringMyObject {

  IssueStringMyObject() {
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  String bar;

  @VisibleForTesting(otherwise = "3")
  String foo;

}
