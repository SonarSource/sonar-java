package org.foo;

public class A {
  public abstract sealed class Shape permits Circle, Rectangle, Square {
    void foo(int non, int sealed) {
      // bugs in ECH - should be valid without spaces
      int permits = non - sealed;
    }

    void foo() { }
  }

  public final class Circle extends Shape { }
  public non-sealed class Rectangle extends Shape { }
  public final class Square extends Shape { }
}
