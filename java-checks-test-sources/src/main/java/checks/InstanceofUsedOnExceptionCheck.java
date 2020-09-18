package checks;

import java.io.IOException;

class InstanceofUsedOnExceptionCheck {
  private void f(MyException foo) {
    try {
    } catch (Exception e) {
      if (e instanceof IOException) {} // Noncompliant [[sc=13;ec=23]] {{Replace the usage of the "instanceof" operator by a catch block.}}
      if (foo instanceof IOException) {}
      if (foo instanceof e) {}
      if (foo.foo instanceof IOException) {}
      if (e
          instanceof IOException) {} // Noncompliant
    }

    Object e = 0;
    if (e instanceof Integer) {}
  }

  private static class MyException extends IOException {
    Exception foo;
  }

  interface e {}
}
