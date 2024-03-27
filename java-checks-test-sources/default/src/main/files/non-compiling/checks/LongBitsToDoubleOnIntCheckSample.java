package checks;

class LongBitsToDoubleOnIntCheckSample {
  public static double getDouble() {
    return Double.longBitsToDouble(Unknown.getSomething()); // Compliant, getSomething can return Long
  }
}
