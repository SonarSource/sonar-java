package checks;

class Switch {
  public void f(String str, char ch) {
    switch (str) {
      case "NEW":
      case "ABC":
      case "NEW_WITH_AT_LEAST_ONE_WORD": {
        newSwitch: // Noncompliant
        switch (ch) {
          // Empty word
          case ',':
            for (int i = 0; i < 10; ++i) {
              if (i > 5) {
                break newSwitch;
              }
            }
        }
        break;
      }
      case "AAAA":
        System.out.println("AAA");
        break;
      default:
        System.out.println("Default");
    }
  }
}
