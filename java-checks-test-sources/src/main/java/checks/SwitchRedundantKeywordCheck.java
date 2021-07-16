package checks;

public class SwitchRedundantKeywordCheck {

  int switchStatement(String mode) {
    // All example are Compliant  this rule does not target "colon label"
    int result = 0;
    switch (mode) {
      case "a": { result = 1; break; }
      case "b": {
        result = 2;
        break;
      }
      default: {
        doSomethingElse();
        result = 3;
        break;
      }
    }

    switch (mode) {
      case "a": result = 1; break; // Compliant
      case "b":
        result = 2;
        break;
      default:
        doSomethingElse();
        result = 3;
        break;
    }

    switch (mode) {
      case "a": result = 1;
      default: {
        doSomethingElse();
        result = 2;
      }
    }
    return result;
  }

  private void doSomethingElse() {
  }
}
