public class Java15SwitchExpression {
  void switchExpression() {
    int i = switch (1) {
      case 2 -> 1;
      default -> 2;
    };
  }
}
