package checks.VisibleForTestingProtectedUsageCheck.CheckOtherwiseString;

public class IssueStringService {

  public String test() {
    return new IssueStringMyObject().foo; // Noncompliant {{Remove this usage of "foo", it is annotated with @VisibleForTesting and should not be accessed from production code.}}
  }

  public String test2() {
    return new IssueStringMyObject().bar; // Noncompliant {{Remove this usage of "bar", it is annotated with @VisibleForTesting and should not be accessed from production code.}}
  }

}
