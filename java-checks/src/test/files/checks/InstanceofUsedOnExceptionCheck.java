import java.io.IOException;

class A {
  private void f() {
    try {
    } catch (Exception e) {
      if (e instanceof IOException) { // Noncompliant {{Replace the usage of the "instanceof" operator by a catch block.}}
      }

      if (foo instanceof IOException) {
      }

      if (foo instanceof e) {
      }

      if (e.foo instanceof IOException) {
      }
      if (e
          instanceof IOException) { // Noncompliant
      }
    }

    int e = 0;

    if (e instanceof Integer) {
    }
  }
}
