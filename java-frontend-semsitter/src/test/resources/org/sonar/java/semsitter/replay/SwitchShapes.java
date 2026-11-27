class SwitchShapes {
  int trailingLabel(int x) {
    int result = 0;
    switch (x) {
      case 1:
        result = 10;
      case 2:
      case 3:
        result++;
        break;
      case 4:
    }
    return result;
  }

  String nullDefault(Object o) {
    return switch (o) {
      case String s -> s;
      case null, default -> "other";
    };
  }

  int colonExpression(String day) {
    return switch (day) {
      case "MON":
      case "TUE":
        yield 1;
      default:
        int length = day.length();
        yield length;
    };
  }

  int ruleValues(int x) {
    return switch (x) {
      case 1, 2 -> x * 2;
      case 3 -> {
        int y = x + 1;
        yield y;
      }
      default -> throw new IllegalArgumentException();
    };
  }
}
