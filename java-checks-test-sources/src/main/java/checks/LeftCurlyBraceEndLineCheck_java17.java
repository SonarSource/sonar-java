package checks;

import static java.util.Objects.requireNonNull;

class LeftCurlyBraceEndLineCheck_java17 {
  record Person(String name) {
    Person
    { // Noncompliant
      requireNonNull(name);
    }
  }

  public record Foo(String x,
    String y,
    String z) { // Compliant
  }

  public record Bar(String x,
    String y,
    String z)
  { // Noncompliant
  }

  public sealed interface Shape
    permits Rectangle, Triangle { // Compliant
  }

  public sealed interface Form
    permits Triangle, Rectangle
  { // Noncompliant
  }

  public static non-sealed class Rectangle implements Shape, Form { }
  public static record Triangle(int a, int b, int c) implements Form, Shape { }
}
