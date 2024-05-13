package checks;

class Switch {
  public void f(String str, char ch) {
    switch (0) {
      case 0:
        break;
      case 1:
        break;
      foo: // Noncompliant {{Remove this misleading "foo" label.}}
//    ^^^
      break;
      bar: // Noncompliant
      break;
      case 2:
        int a = 0;
        break;
      default:
        break;
    }

    switch (str) {
      case "NEW":
      case "NEW_WITH_AT_LEAST_ONE_WORD": {
        newSwitch: // Noncompliant
        switch (ch) {
          // Empty word
          case ',':
        }
      }
    }
  }
}
