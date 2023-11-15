package checks;

class LongBitsToDoubleOnIntCheck {
  public static double getDouble() {
    return Double.longBitsToDouble(Unknown.getSomething()); // Compliant, getSomething can return Long
  }
}
