package checks;

public class SwitchCasesShouldBeCommaSeparatedCheck {

  public void nonCompliant(String mode) {
    // Switch Statement
    switch (mode) {
      case "a":
        doSomething();
        break;
      case "b":
      case "c": // Noncompliant [[sc=7;ec=16;secondary=-1]] {{Merge the previous cases into this one using comma-separated label.}}
        doSomething();
        doSomethingElse();
        break;
      default:
        doSomethingElse();
    }
  }

  private void doSomething() {
  }

  private void doSomethingElse() {
  }
}
