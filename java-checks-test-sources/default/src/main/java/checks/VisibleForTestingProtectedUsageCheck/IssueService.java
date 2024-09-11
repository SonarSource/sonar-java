package checks.VisibleForTestingProtectedUsageCheck;

public class IssueService {

  public String test() {
    return new IssueMyObject().foo; // Noncompliant {{Remove this usage of "foo", it is annotated with @VisibleForTesting and should not be accessed from production code.}}
  }

}
