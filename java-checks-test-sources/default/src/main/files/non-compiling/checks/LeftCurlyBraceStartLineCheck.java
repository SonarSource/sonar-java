class NestedSwitchStatementCheck
{
  void switchExpressions(MyEnum myEnum)
  {
    int i = switch (myEnum)
      {                                   // Compliant
        case A:
          yield 1;
        default:
          yield 2;
      };

    int j = switch (myEnum) {             // Noncompliant {{Move this left curly brace to the beginning of next line of code.}}
      case A:
        yield 1;
      default:
        yield 2;
    };
  }

  enum MyEnum
  {A, B}
}
