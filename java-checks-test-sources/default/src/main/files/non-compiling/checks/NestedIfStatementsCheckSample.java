class NestedSwitchStatementCheck {
  void switchExpressions(int i, boolean cond) {
    int j = switch (i) { // Compliant - 1
      case 1: {
        if (cond) { // Compliant - 2
          switch (i) { // Compliant - 3
            case 1 -> {
              if (false) { // Noncompliant {{Refactor this code to not nest more than 3 if/for/while/switch/try statements.}}
              }
            }
          }
        }
        yield 1;
      }
      default:
        yield 2;
    };
  }
}
