class NestedBlocksCheck {
  int switchInJava14(String mode) {
    int i = switch (mode) {
      case "a" -> {        // Compliant
        yield 1;
      }
      default -> {         // Compliant
        doSomethingElse();
        yield 2;
      }
    };

    int result = 0;

    switch (mode) {
      case "a" -> {       // Compliant
        result = 1;
        break;
      }
      default -> {         // Compliant
        doSomethingElse();
        result = 2;
        break;
      }
    };

    return i + switch (mode) {
      case "a":
      { // Compliant
        yield 1;
      }
      case "b":
      { // Compliant
        doSomethingElse();
      }
      yield 1;
      case "c":
      { // Noncompliant
        doSomethingElse();
      }
      { // Noncompliant
        doSomethingElse();
      }
      yield 1;
      default: {
        yield 2;
      }
    };
  }

  private void doSomethingElse() {
  }
}
