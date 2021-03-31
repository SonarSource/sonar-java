package checks;

public class SwitchCasesShouldBeCommaSeparatedCheck {

  public void nonCompliant(String mode) {
    // Switch Expression
    int i = switch (mode) {
      case "a":
      case "b": // Noncompliant [[sc=7;ec=16;secondary=-1]] {{Merge the previous cases into this one using comma-separated label.}}
        yield 1;
      default:
        yield 3;
    };
  }

  public void compliant(String mode) {
    // Switch Expression
    int i = switch (mode) {
      case "a", "b":
        yield 1;
      default:
        yield 3;
    };

    // Or even better:
    switch (mode) {
      case "a", "b" -> doSomething();
      default -> doSomethingElse();
    }
  }
}
