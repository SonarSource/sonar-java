package checks;

public class SwitchCasesShouldBeCommaSeparatedCheckSample {

  public void nonCompliant(String mode) {
    // Switch Statement
    switch (mode) {
      case "a":
        doSomething();
        break;
      case "b":
//  ^^^<
      case "c": // Noncompliant {{Merge the previous cases into this one using comma-separated label.}}
//    ^^^^^^^^^
        doSomething();
        doSomethingElse();
        break;
      default:
        doSomethingElse();
    }

    // Switch Statement with multiple issues
    switch (mode) {
      case "a":
//  ^^^<
      case "b":
//  ^^^<
      case "c": // Noncompliant {{Merge the previous cases into this one using comma-separated label.}}
//    ^^^^^^^^^
        doSomething();
        break;
      case "d":
//  ^^^<
      case "e": // Noncompliant {{Merge the previous cases into this one using comma-separated label.}}
//    ^^^^^^^^^
      default:
        doSomethingElse();
    }
  }

  public void compliant(String mode) {
    // Empty switch statement
    switch (mode) {
    }
    // Switch case with default only
    switch (mode) {
      default:
    }
    // Switch case with empty case
    switch (mode) {
      case "a":
    }
    // Switch case with empty case and default
    switch (mode) {
      case "a":
      default:
        doSomething();
    }

    // Uncommon cases ordering
    switch (mode) {
      default:
      case "a":
        doSomething();
    }
  }

  private void doSomething() {
  }

  private void doSomethingElse() {
  }
}
