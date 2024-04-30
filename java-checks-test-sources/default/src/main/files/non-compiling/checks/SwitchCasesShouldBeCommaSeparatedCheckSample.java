package checks;

public class SwitchCasesShouldBeCommaSeparatedCheckSample {

  public void nonCompliant(String mode) {
    // Switch Expression
    int i = switch (mode) {
      case "a":
        yield 0;
      case "b":
//    ^^^^^^^^^>
      case "c": // Noncompliant {{Merge the previous cases into this one using comma-separated label.}}
//    ^^^^^^^^^
        yield 42;
      default:
        yield returnSomethingElse();
    };

    // Switch expression with multiple issues
    i = switch (mode) {
      case "a":
//    ^^^^^^^^^>
      case "b":
//    ^^^^^^^^^>
      case "c": // Noncompliant {{Merge the previous cases into this one using comma-separated label.}}
//    ^^^^^^^^^
        yield 42;
      case "d":
//    ^^^^^^^^^>
      case "e": // Noncompliant {{Merge the previous cases into this one using comma-separated label.}}
//    ^^^^^^^^^
      default:
        yield returnSomethingElse();
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
    i = switch (mode) {
      case "a", "b" -> returnSomething();
      default -> returnSomethingElse();
    };

    // Uncommon cases ordering
    i = switch (mode) {
      default:
      case "a":
        yield 42;
    };
  }
  private int returnSomething() {
    return -1;
  }

  private int returnSomethingElse() {
    return -42;
  }
}
