package checks;

class UnnecessaryBitOperationCheck {
  private void foo() {
    int result;
    int bitMask = 0x000F;

    result = bitMask & -1; // Noncompliant {{Remove this unnecessary bit operation.}}
    result = bitMask | 0;  // Noncompliant [[sc=22;ec=23]] {{Remove this unnecessary bit operation.}}
    result = bitMask ^ 0;  // Noncompliant {{Remove this unnecessary bit operation.}}
    result &= -1; // Noncompliant [[sc=12;ec=14]] {{Remove this unnecessary bit operation.}}
    result |= 0;  // Noncompliant {{Remove this unnecessary bit operation.}}
    result ^= 0;  // Noncompliant {{Remove this unnecessary bit operation.}}

    result = bitMask & 1; // Compliant
    result = bitMask | 1; // compliant
    result = bitMask ^ 1; // Compliant
    result &= 1; // Compliant
    result |= 1; // compliant
    result ^= 1; // Compliant

    long bitMaskLong = 0x000F;
    long resultLong;
    resultLong = bitMaskLong & -1l; // Noncompliant {{Remove this unnecessary bit operation.}}
    resultLong = bitMaskLong & 0L; // Compliant
    resultLong = bitMaskLong & returnLong(); // Compliant
    resultLong = bitMaskLong & 0x0F; // Compliant
    
    resultLong = bitMaskLong & 0xFFFFFFFFFFFFFFFFL; // Compliant
    resultLong = bitMaskLong & 0xFFFFFFFFFFFFFFFEL; // Compliant
    resultLong = bitMaskLong & 0x8000000000000000L; // Compliant
    resultLong = 0x8000000000000000L & bitMaskLong; // Compliant
  }
  
  private long returnLong() {
    return Long.valueOf(1L);
  }
}
