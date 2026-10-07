package org.foo;

public class A {
  public sealed interface Shape permits Circle, Rectangle, Square, Diamond {
    default void foo(int non, int sealed) {

      int permits = non - sealed;
    }

    default void foo() { }
  }

  public final class Circle implements Shape { }
  public non-sealed class Rectangle implements Shape { }
  public final class Square implements Shape { }
  public record Diamond() implements Shape { }
}
