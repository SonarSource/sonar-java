import javax.annotation.Nullable;

class A {
  void max(double a, double b, @Nullable Object o) {
    double c = Math.max(a, b);
    if (c == a || c == b) {
      return;
    }
    // unreachable
    o.toString();
  }

  void max(float a, float b, @Nullable Object o) {
    float c = Math.max(a, b);
    if (c == a || c == b) {
      return;
    }
    // unreachable
    o.toString();
  }

  void max(int a, int b, @Nullable Object o) {
    int c = Math.max(a, b);
    if (c == a || c == b) {
      return;
    }
    // unreachable
    o.toString();
  }

  void max(long a, long b, @Nullable Object o) {
    long c = Math.max(a, b);
    if (c == a || c == b) {
      return;
    }
    // unreachable
    o.toString();
  }

  void min(double a, double b, @Nullable Object o) {
    double c = Math.min(a, b);
    if (c == a || c == b) {
      return;
    }
    // unreachable
    o.toString();
  }

  void min(float a, float b, @Nullable Object o) {
    float c = Math.min(a, b);
    if (c == a || c == b) {
      return;
    }
    // unreachable
    o.toString();
  }

  void min(int a, int b, @Nullable Object o) {
    int c = Math.min(a, b);
    if (c == a || c == b) {
      return;
    }
    // unreachable
    o.toString();
  }

  void min(long a, long b, @Nullable Object o) {
    long c = Math.min(a, b);
    if (c == a || c == b) {
      return;
    }
    // unreachable
    o.toString();
  }
}
