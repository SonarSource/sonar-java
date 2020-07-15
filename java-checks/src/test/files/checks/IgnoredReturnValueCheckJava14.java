class SwitchExpression {

  void example(String condition, String s) {
    s = switch (condition) {
      case "1" -> s.toString();
      default -> "";
    };

    s = switch (condition) {
      case "1":
        yield s.toString();
      default:
        yield "";
    };

    switch (condition) {
      case "1":
        s.toString(); // Noncompliant {{The return value of "toString" must be used.}}
        break;
    }
  }

}
