import java.util.List;

class Patterns {

  sealed interface Shape permits Circle, Rect {
  }

  record Circle(double radius) implements Shape {
  }

  record Rect(double width, double height) implements Shape {
  }

  enum Color {
    RED, GREEN, BLUE
  }

  double area(Shape shape) {
    return switch (shape) {
      case Circle c -> Math.PI * c.radius() * c.radius();
      case Rect(double w, double h) -> w * h;
    };
  }

  String describe(Object o) {
    if (o instanceof String s && !s.isEmpty()) {
      return s;
    }
    if (o instanceof Rect(var w, var h)) {
      return w + "x" + h;
    }
    return switch (o) {
      case null -> "null";
      case Integer i when i > 10 -> "big";
      case Integer i -> "int " + i;
      case List<?> list -> "list " + list.size();
      default -> o.toString();
    };
  }

  int code(Color color) {
    return switch (color) {
      case RED -> 1;
      case GREEN -> 2;
      case BLUE -> {
        int base = 3;
        yield base;
      }
    };
  }

  String text(String s) {
    String block = """
      block
      text
      """;
    return switch (s) {
      case "a", "b" -> block;
      default -> s;
    };
  }
}
