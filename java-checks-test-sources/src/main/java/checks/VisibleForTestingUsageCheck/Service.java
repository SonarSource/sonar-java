package checks.VisibleForTestingUsageCheck;

public class Service {

  public String f() {
    return new MyObject().foo; // Noncompliant {{Remove this usage of "foo", it is annotated with @VisibleForTesting and should not be accessed from production code.}}
  }

}
