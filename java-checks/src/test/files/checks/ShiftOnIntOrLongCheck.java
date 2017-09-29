class Shifts {
  void ignoreZeroWhenAligned() {
    byte b1, b2;
    b2 = (byte) (value >> 0); // Compliant, because of formatting
    b1 = (byte) (value >> 8);
  }

  public int shift(int a) {
    int b;
    b = a <<  31;
    b = a >> -31;
    b = a <<  32; // Noncompliant {{Remove this useless shift}}
    b = a >> -32; // Noncompliant [[sc=11;ec=13]] {{Remove this useless shift}}
    b = a <<  33; // Noncompliant {{Either make "a" a "long" or correct this shift to 1}}
    b = a >> -33; // Noncompliant {{Either make "a" a "long" or correct this shift to -1}}

    a <<=  31;
    a >>= -31;
    a <<=  32; // Noncompliant
    a >>= -32; // Noncompliant
    a <<=  33; // Noncompliant
    a >>= -33; // Noncompliant
    return b << +48; // Noncompliant
  }

  public long shift(long a) {
    long b;
    b = a >>  63;
    b = a << -63;
    b = a >>  64; // Noncompliant
    b = a << -64; // Noncompliant
    b = a >>  65; // Noncompliant {{Correct this shift to 1}}
    b = a << -65; // Noncompliant {{Correct this shift to -1}}

    a >>=  63;
    a <<= -63;
    a >>=  64; // Noncompliant
    a <<= -64; // Noncompliant
    a >>=  65; // Noncompliant
    a <<= -65; // Noncompliant [[sc=7;ec=10]]
    return b >> +96; // Noncompliant
  }

  public long shiftOtherCases(long a, long b) {
    long c;
    long[] d = new long[]{1L};
    Long[] e = new Long[]{1L};
    c = a >> b;
    c = a << (b + 3);
    c = a >> returnLong(); //Compliant
    c = (a - 3) << (b + 3);
    c = (a - 3) >> 63;
    c = (a - 3) << 64; // Noncompliant
    c = (a - 3) >> 96; // Noncompliant
    c = returnLong() << 97; // Noncompliant
    c = d[0] >> 98; // Noncompliant 
    c = e[0] << 99; // Noncompliant
    c = a >> 0x0009;
    c = a << 0x0000; // Noncompliant
    return c;
  }

  public int shiftOtherCases(int a, int b) {
    int c;
    int[] d = new int[]{1};
    Integer[] e = new Integer[]{1};
    c = a << b;
    c = a >> (b + 3);
    c = a << returnInt();
    c = (a - 3) >> (b + 4);
    c = (a - 3) << 31;
    c = (a - 3) >> 32; // Noncompliant
    c = (a - 3) << 48; // Noncompliant
    c = returnInt() >> 49; // Noncompliant
    c = d[0] << 50; // Noncompliant
    c = e[0] >> 51; // Noncompliant
    c = a << 0x0009;
    c = a >> 0x0000; // Noncompliant
    return c;
  }
  
  public void cornerCase() {
    int a;
    a = 1 << 0;
    a = 1 << 1;
    a = 1 << 1L;
    a = 1 << 1l;
    a = 0xfffffffffffffffeL << 7; 
    a = 0xffffffffffffffffL << 7; 
    a = 0x8000000000000000L << 7; 
    a = 1 << 0x8000000000000000L; 
  }

  public int returnInt() {
    return 0;
  }

  public long returnLong() {
    return 0L;
  }

  void ignoreZeroWhenAligned() {
    byte b1, b2;
    b1 = (byte) (value >> 8);
    b2 = (byte) (value >> 0); // Compliant, because of formatting
  }

  void aligned() {
    b1 = (byte) (value >> 8);
    b2 = (byte) (value >> 0); // Compliant
    b2 = (byte) (value << 0); // Noncompliant - another type of shift
    System.out.println();
    b2 = (byte) (value >> 0); // Noncompliant - aligned but interrupted
    System.out.println();
    System.out.println();
    b2 = (byte) (value >> 0); // Noncompliant  - aligned but interrupted
    b2 = (byte) (value    >> 0); // Noncompliant  - aligned but interrupted
  }
}
