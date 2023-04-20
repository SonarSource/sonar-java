package org.foo;

/**
 * Requires Java 17 (JEP-406) preview feature 
 */
public class SwitchWithPatterns {
  static class A {}

  public static void main(String[] args) {
    System.out.println(String.format("%d,%d,%d",
      switch_array_null_pattern(null),
      switch_array_null_pattern(new A()),
      switch_array_null_pattern(new A[10])));

    System.out.println(String.format("%d,%d,%d",
      switch_array_default_null_pattern(null),
      switch_array_default_null_pattern(new A()),
      switch_array_default_null_pattern(new A[10])));
  }

  static Object foo(Object o) {
    return switch (o) {
      default -> o;
    };
  }

  static int switch_array_null_pattern(Object o) {
    return switch (o) {
      case Object[] arr -> arr.length;
      case null -> 42;
      default -> -1;
    };
  }

  static int switch_array_default_null_pattern(Object o) {
    return switch (o) {
      case Object[] arr -> arr.length;
      case null, default -> 42;
    };
  }

  static String switch_sealed_class_minimum(Shape shape) {
    return switch (shape) {
      case Triangle t -> "triangle";
      case Rectangle r -> "rectangle";
    };
  }

  static String switch_sealed_class_null_default_sub_classes(Shape shape) {
    return switch (shape) {
      case null -> "null case";
      case Triangle t -> String.format("triangle (%d,%d,%d)", t.a(), t.b(), t.c());
      case Rectangle r when r.volume() > 42 -> String.format("big rectangle of volume %d!", r.volume());
      case Square s -> "Square!";
      case Rectangle r -> String.format("Rectangle (%d,%d)", r.base, r.height);
      default -> "default case";
    };
  }

  public sealed interface Shape permits Rectangle,Triangle {
    default int volume() { return 0; }
  }

  public static non-sealed class Rectangle implements Shape {
    private int base, height;
    Rectangle(int base, int height) { this.base = base; this.height = height; }
  }

  public static final class Square extends Rectangle {
    Square(int side) { super(side, side); }
  }

  public static record Triangle(int a, int b, int c) implements Shape {}
}
