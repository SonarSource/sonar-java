package javax.annotation;

@interface CheckForNull {}
public class TempTestClass {

  public static void castedComparison(String string, int count) {
    final int len = string.length();
    final long longSize = (long) len * (long) count;
    final int size = (int) longSize;
    //FP because of casting and type difference, we can actually end up with diff value but we'll live with it for now.
    if (size != longSize) { // Noncompliant
      throw new ArrayIndexOutOfBoundsException("Required array size too large: " + longSize);
    }
  }

  private void bigToDouble(BigInteger x) {
    BigInteger absX = x.abs();
    int shift = 1;
    long twiceSignifFloor = absX.shiftRight(shift).longValue();
    long signifFloor = twiceSignifFloor >> 1;
    signifFloor &= SIGNIFICAND_MASK; // remove the implied bit
    boolean increment = (twiceSignifFloor & 1) != 0
        && ((signifFloor & 1) != 0 || absX.getLowestSetBit() < shift);
    //FP - SONARJAVA-???
    long signifRounded = increment ? signifFloor + 1 : signifFloor;
  }

}
