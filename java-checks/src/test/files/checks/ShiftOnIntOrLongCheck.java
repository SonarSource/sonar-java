class Shifts {
  public int shift(int a) {
    int b;
    b = a <<  31; // Compliant
    b = a >> -31; // Compliant
    b = a <<  32; // Nomcompliant
    b = a >> -32; // Noncompliant
    b = a <<  33; // Noncompliant
    b = a >> -33; // Noncompliant

    a <<=  31; // Compliant
    a >>= -31; // Compliant
    a <<=  32; // Noncompliant
    a >>= -32; // Noncompliant
    a <<=  33; // Noncompliant
    a >>= -33; // Noncompliant
    return b << +48; // Noncompliant
  }

  public long shift(long a) {
    long b;
    b = a >>  63; // Compliant
    b = a << -63; // Compliant
    b = a >>  64; // Nomcompliant
    b = a << -64; // Noncompliant
    b = a >>  65; // Noncompliant
    b = a << -65; // Noncompliant

    a >>=  63; // Compliant
    a <<= -63; // Compliant
    a >>=  64; // Noncompliant
    a <<= -64; // Noncompliant
    a >>=  65; // Noncompliant
    a <<= -65; // Noncompliant
    return b >> +96; // Noncompliant
  }

  public long shiftOtherCases(long a, long b) {
    long c;
    long[] d = new long[]{1L};
    Long[] e = new Long[]{1L};
    c = a >> b; // Compliant
    c = a << (b + 3); // Compliant
    c = a >> returnLong(); //Compliant
    c = (a - 3) << (b + 3); // Compliant
    c = (a - 3) >> 63; // Compliant
    c = (a - 3) << 64; // Noncompliant
    c = (a - 3) >> 96; // Noncompliant
    c = returnLong() << 97; // Noncompliant
    c = d[0] >> 98; // Noncompliant 
    c = e[0] << 99; //Noncompliant
    c = a >> 0x0009; // Compliant
    c = a << 0x0000; // Noncompliant
    return c;
  }

  public int shiftOtherCases(int a, int b) {
    int c;
    int[] d = new int[]{1};
    Integer[] e = new Integer[]{1};
    c = a << b; // Compliant
    c = a >> (b + 3); // Compliant
    c = a << returnInt(); // Compliant
    c = (a - 3) >> (b + 4); // Compliant
    c = (a - 3) << 31; // Compliant
    c = (a - 3) >> 32; // Noncompliant
    c = (a - 3) << 48; // Noncompliant
    c = returnInt() >> 49; // Noncompliant
    c = d[0] << 50; // Noncompliant
    c = e[0] >> 51; // Noncompliant
    c = a << 0x0009; // Compliant
    c = a >> 0x0000; // Noncompliant
    return c;
  }
  
  public void cornerCase() {
    int a;
    a = 1 << 0; // Compliant
    a = 1 << 1; // Compliant
    a = 1 << 1L; // Compliant
    a = 1 << 1l; // Compliant
    a = 0xfffffffffffffffeL << 7;  // Compliant
    a = 0xffffffffffffffffL << 7;  // Compliant
    a = 0x8000000000000000L << 7;  // Compliant
    a = 1 << 0x8000000000000000L;  // Compliant
  }

  public int returnInt() {
    return 0;
  }

  public long returnLong() {
    return 0L;
  }
}