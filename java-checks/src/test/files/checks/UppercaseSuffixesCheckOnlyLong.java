class A {
  double f = 1.;
  long long1 = 1l; // Noncompliant [[sc=16;ec=18]] {{Upper-case this literal "l" suffix.}}
  float float1 = 1.0f; // Compliant
  double double1 = 1.0d; // Compliant

  private void test () {

    long retVal = (bytes[0] & 0xFF);
    for (int i = 1; i < Math.min(bytes.length, 8); i++) {
      retVal |= (bytes[i] & 0xFFL) << (i * 8);
    }
  }
}
