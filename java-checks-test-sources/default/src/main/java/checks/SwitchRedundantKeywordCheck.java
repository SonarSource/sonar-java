package checks;

public class SwitchRedundantKeywordCheck {

  int switchExpression(String mode) {
    int nonCompliant = switch (mode) {
      case "a" -> {
        yield 1; // Noncompliant [[sc=9;ec=14;secondary=-1,+1]] {{Remove this redundant block and "yield".}}
      }
      default -> {
        yield 2; // Noncompliant {{Remove this redundant block and "yield".}}
      }
    };

    int compliant1 = switch (mode) {
      case "a" -> 1; // Compliant
      case "b" -> {
        doSomethingElse();
        yield 1; // Compliant
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
      case "a" -> { // Noncompliant
        result = 1;
      }
      case "d" -> { // Noncompliant
        throw new IllegalArgumentException();
      }
      case "a1" -> { // compliant, two statements in block
        mode = "";
        result = 1;
      }
      case "b" -> { // compliant
        if (mode.equals("b")) {
          doSomethingElse();
        }
      }
      case "c" -> {
        return 1;
      }
      default -> doSomethingElse();
    }

    switch (mode) {
      case "a" -> {
        result = 1;
        break; // Noncompliant [[sc=9;ec=15;secondary=-2,+1]] {{Remove this redundant block and "break".}}
      }
      case "b" -> {
        result = 2;
        break; // Noncompliant {{Remove this redundant block and "break".}}
      }
      default -> {
        doSomethingElse();
        result = 3;
        break; // Noncompliant [[sc=9;ec=15]] {{Remove this redundant "break".}}
      }
    }

    my_for:
    for (int i = 0; i < 10; i++) {
      switch (i) {
        case 9 -> {
          System.out.println("Hello");
          break my_for; // Compliant when break with label
        }
        default -> System.out.println("Splash!");
      }
    }

    switch (mode) {
      case "a" -> result = 1; // Compliant
      case "b" -> { // Noncompliant [[sc=19;ec=20;secondary=+2]] {{Remove this redundant block.}}
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
