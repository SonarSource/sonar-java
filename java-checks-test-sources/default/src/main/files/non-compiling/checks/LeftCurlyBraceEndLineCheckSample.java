class NestedSwitchStatementCheck {
  void switchExpressions(MyEnum myEnum) {
    int i = switch (myEnum)
      { // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
        case A:
          yield 1;
        default:
          yield 2;
      };

    int j = switch (myEnum) {               // Compliant
      case A:
        yield 1;
      default:
        yield 2;
    };
  }

  enum MyEnum {A, B}
}
