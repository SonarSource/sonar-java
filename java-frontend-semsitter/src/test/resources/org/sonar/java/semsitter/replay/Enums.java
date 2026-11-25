import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

enum Enums implements Comparable<Enums>, Runnable {
  PLUS("+", 1) {
    @Override
    int apply(int a, int b) {
      return a + b;
    }
  },
  MINUS("-", 1) {
    @Override
    int apply(int a, int b) {
      return a - b;
    }
  },
  TIMES("*") {
    @Override
    int apply(int a, int b) {
      return a * b;
    }
  };

  private final String symbol;
  private final int priority;

  Enums(String symbol) {
    this(symbol, 2);
  }

  Enums(String symbol, int priority) {
    this.symbol = symbol;
    this.priority = priority;
  }

  abstract int apply(int a, int b);

  @Override
  public void run() {
    System.out.println(symbol + priority);
  }

  static Enums parse(String text) {
    for (Enums value : values()) {
      if (value.symbol.equals(text)) {
        return value;
      }
    }
    return valueOf(text);
  }

  int compute(int a, int b) {
    switch (this) {
      case PLUS:
        return PLUS.apply(a, b);
      case MINUS:
        return apply(a, b);
      default:
        return name().length() + ordinal();
    }
  }

  static Map<Enums, Integer> table() {
    Map<Enums, Integer> table = new EnumMap<>(Enums.class);
    EnumSet.allOf(Enums.class).forEach(value -> table.put(value, value.priority));
    return table;
  }
}
