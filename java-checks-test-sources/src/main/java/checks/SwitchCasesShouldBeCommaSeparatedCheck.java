package checks;

public class SwitchCasesShouldBeCommaSeparatedCheck {

  public void nonCompliant(String mode) {
    // Switch Statement
    switch (mode) {
      case "a":
      case "b": // Noncompliant {{Merge the previous cases into this one using comma-separated label.}}
        doSomething();
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
