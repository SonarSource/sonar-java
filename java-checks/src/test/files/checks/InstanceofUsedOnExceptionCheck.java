import java.io.IOException;

class A {
  private void f() {
    try {
    } catch (Exception e) {
      if (e instanceof IOException) { // Non-Compliant
      }

      if (foo instanceof IOException) { // Compliant
      }

      if (foo instanceof e) { // Compliant
      }

      if (e.foo instanceof IOException) { // Compliant
      }
      if (e
          instanceof IOException) { // Non-Compliant
      }
    }

    int e = 0;

    if (e instanceof Integer) { // Compliant
    }
  }
}
