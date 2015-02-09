class A {
  private void foo() {
    int result;
    int bitMask = 0x000F;

    result = bitMask & -1; // Noncompliant
    result = bitMask | 0;  // Noncompliant
    result = bitMask ^ 0;  // Noncompliant
    result &= -1; // Noncompliant
    result |= 0;  // NonCompliant
    result ^= 0;  // Noncompliant

    result = bitMask & 1; // Compliant
    result = bitMask | 1; // compliant
    result = bitMask ^ 1; // Compliant
    result &= 1; // Compliant
    result |= 1; // compliant
    result ^= 1; // Compliant

    long bitMaskLong = 0x000F;
    long resultLong;
    resultLong = bitMaskLong & -1l; // Noncompliant
    resultLong = bitMaskLong & 0L; // Compliant
    resultLong = bitMaskLong & returnLong(); // Compliant
    resultLong = bitMaskLong & 0x0F; // Compliant
  }
  
  private long returnLong() {
    return Long.valueOf(1L);
  }
}
