import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

class Statements {

  private final Object lock = new Object();

  int run(String s, List<String> items) throws IOException {
    int total = 0;
    final int limit = 10;
    var inferred = "local";
    for (int i = 0, j = 1; i < s.length(); i++, j++) {
      if (s.charAt(i) == 'x') {
        continue;
      } else if (s.charAt(i) == 'y') {
        break;
      } else {
        total += j;
      }
    }
    for (String item : items) {
      total += item.length();
    }
    for (;;) {
      break;
    }
    while (total > limit) {
      total--;
    }
    do {
      total++;
    } while (total < 3);
    outer:
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (i == j) {
          continue outer;
        }
        if (i > j) {
          break outer;
        }
      }
    }
    switch (s) {
      case "a":
      case "b":
        total = 1;
        break;
      case "c":
        total = 2;
      default:
        total = 3;
    }
    switch (total) {
      case 1 -> total = 10;
      case 2, 3 -> {
        total = 20;
      }
      default -> throw new IllegalStateException();
    }
    try (Reader reader = new FileReader(s); Reader other = new FileReader(inferred)) {
      total += reader.read() + other.read();
    } catch (IllegalStateException | IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw e;
    } finally {
      total = 0;
    }
    synchronized (lock) {
      total++;
    }
    assert total >= 0 : "negative";
    ;
    {
      int scoped = 1;
      total += scoped;
    }
    class Local {
      int value() {
        return limit;
      }
    }
    total += new Local().value();
    return total;
  }

  void nothing() {
    return;
  }

  int yields(int k) {
    return switch (k) {
      case 1:
        yield 10;
      default:
        yield 0;
    };
  }
}
