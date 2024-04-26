class A {
  double f = 1.;
  long long1 = 1l; // Noncompliant {{Upper-case this literal "l" suffix.}}
//             ^^
  float float1 = 1.0f; // Noncompliant {{Upper-case this literal "f" suffix.}}
  double double1 = 1.0d; // Noncompliant {{Upper-case this literal "d" suffix.}}

  private void test () {

    long retVal = (bytes[0] & 0xFF);
    for (int i = 1; i < Math.min(bytes.length, 8); i++) {
      retVal |= (bytes[i] & 0xFFL) << (i * 8);
    }
  }
}
