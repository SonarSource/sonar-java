package checks;

class LongBitsToDoubleOnIntCheckSample {
  void foo() {
    byte b = 1;
    short s = 1;
    Double.longBitsToDouble('c'); // Noncompliant [[sc=12;ec=28]] {{Remove this "Double.longBitsToDouble" call.}}
    Double.longBitsToDouble(s); // Noncompliant
    Double.longBitsToDouble(b); // Noncompliant
    Double.longBitsToDouble(1); // Noncompliant
    Double.longBitsToDouble(1L);
    Double.longBitsToDouble(Long.valueOf(1l));
  }

  static class callOther {
    public static double getDouble() {
      return Double.longBitsToDouble(Other.getLong()); // Compliant
    }
    public static double getByte() {
      return Double.longBitsToDouble(Other.getByte()); // Noncompliant
    }
  }

  static class Other {
    public static long getLong() {
      return Long.MAX_VALUE;
    }

    public static byte getByte() {
      return 1;
    }
  }
}
