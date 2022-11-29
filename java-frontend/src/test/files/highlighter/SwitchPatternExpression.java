package org.foo;

public class SwitchPatternExpression {

  String foo(Object o) {
    return switch(o) {
      case Point p when p.when() -> "Point";
      case Point(int x, int y) when x == 1 && y == 2 -> "(1,2)";
      default -> "Unsupported";
    };
  }

  record Point(int x, int y) {
    boolean when() { return false; }
  }

}
