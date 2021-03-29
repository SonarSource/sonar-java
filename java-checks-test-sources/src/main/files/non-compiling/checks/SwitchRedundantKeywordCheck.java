package checks;

public class SwitchRedundantKeywordCheck {

  int switchExpression(String mode) {
    int nonCompliant = switch (mode) {
      case "a" -> { yield 1; } // Noncompliant [[sc=19;ec=31]] {{Remove this redundant block and "yield".}}
      default -> {             // Noncompliant {{Remove this redundant block and "yield".}}
        yield 2;
      }
    };

    int compliant1 = switch (mode) {
      case "a" -> 1; // Compliant
      case "b" -> { // Compliant
        doSomethingElse();
        yield 1;
      }
      default -> 2;  // Compliant
    };

    // Compliant examples, this rule does not target "colon label"
    int compliant2 = switch (mode) {
      case "a": { yield 1; }
      default: {
        yield 2;
      }
    };

    int compliant3 = switch (mode) {
      case "a":
        yield 1;
      default:
        yield 2;
    };

    return nonCompliant + compliant1 + compliant2 + compliant3;
  }


  int switchStatement(String mode) {
    int result = 0;
    switch (mode) {
      case "a" -> { result = 1; break; } // Noncompliant [[sc=19;ec=41]] {{Remove this redundant block and "break".}}
      case "b" -> { // Noncompliant {{Remove this redundant block and "break".}}
        result = 2;
        break;
      }
      default -> {
        doSomethingElse();
        result = 3;
        break; // Noncompliant [[sc=9;ec=15]] {{Remove this redundant "break".}}
      }
    }

    switch (mode) {
      case "a" -> result = 1; // Compliant
      case "b" -> { // Noncompliant [[sc=19;ec=20]] {{Remove this redundant block.}}
        result = 2;
      }
      case "c" -> { // Compliant, probably not the best code, but no choice to add an empty block if you want a case that does nothing.
      }
      default -> { // Compliant
        doSomethingElse();
        result = 2;
      }
    }

    // Compliant examples, this rule does not target "colon label"
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
