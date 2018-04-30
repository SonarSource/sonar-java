class A {
  int foo1(String path) { // Compliant
    switch (path) {
      case "FOO":
        return 1;
      case "BAR":
        return 2;
      case "FIZ":
        return 3;
      default:
        return 4;
    }
    return 5;
    return 6;
    return 7;
  }

  int foo2(String path) { // Noncompliant {{Reduce the number of returns of this method 4, down to the maximum allowed 3.}}
    switch (path) {
      case "FOO":
        return 1;
      case "BAR":
        return 2;
      case "FIZ":
        return 3;
      default:
        return 4;
    }
    return 5;
    return 6;
    return 7;
    return 8;
  }

  int foo3(String path) { // Compliant
    switch (path) {
      case "FOO":
        return 1;
      default:
        switch (path) {
          case "BAR":
            return 2;
          default:
            return 3;
        }
    }
    return 4;
    return 5;
    return 6;
  }

  int foo4(String path) { // Noncompliant {{Reduce the number of returns of this method 4, down to the maximum allowed 3.}}
    switch (path) {
      case "FOO":
        return 1;
      default:
        switch (path) {
          case "BAR":
            return 2;
          default:
            return 3;
        }
    }
    return 4;
    return 5;
    return 6;
    return 7;
  }

  int foo5(String path) { // Compliant
    switch (path) {
      case "FOO":
        return 1;
      default:
        switch (path) {
          case "BAR":
            return 2;
          default:
            return 3;
        }
        return 4;
        return 5;
        return 6;
        return 7;
    }
    return 8;
    return 9;
    return 10;
  }

  int foo6(String path) { // Noncompliant {{Reduce the number of returns of this method 4, down to the maximum allowed 3.}}
    switch (path) {
      case "FOO":
        return 1;
      default:
        switch (path) {
          case "BAR":
            return 2;
          default:
            return 3;
        }
        return 4;
      return 5;
      return 6;
      return 7;
    }
    return 8;
    return 9;
    return 10;
    return 11;
  }
}
