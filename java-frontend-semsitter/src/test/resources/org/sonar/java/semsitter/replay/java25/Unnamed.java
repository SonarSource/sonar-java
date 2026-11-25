import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.function.BiFunction;

class Unnamed {
  record Point(int x, int y) {
  }

  private final StringReader shared = new StringReader("shared");

  int count(List<String> items) {
    int total = 0;
    for (var _ : items) {
      total++;
    }
    return total;
  }

  int parse(String text) {
    try {
      return Integer.parseInt(text);
    } catch (NumberFormatException _) {
      return -1;
    }
  }

  int first(Object o) {
    if (o instanceof Point(int x, _)) {
      return x;
    }
    return switch (o) {
      case Point(_, int y) -> y;
      default -> 0;
    };
  }

  BiFunction<String, Integer, String> keepFirst() {
    return (s, _) -> s;
  }

  int read() throws IOException {
    StringReader local = new StringReader("local");
    try (local; this.shared) {
      return local.read() + shared.read();
    }
  }
}
