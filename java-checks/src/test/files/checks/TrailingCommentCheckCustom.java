class Foo {
  int i = 0,
  // Noncompliant@+1
    j = 0, // This is non-compliant
    k = 0;

  // This is compliant
  // Noncompliant@+1
  int a = 0; //    Compliant
  // Noncompliant@+1
  int b = 0; /*    Compliant   */
  // Noncompliant@+1
  int c = 0; // This is non-compliant
  // Noncompliant@+1
  int d = 0; // This is also non-compliant

  int e = /* Compliant */ 0;
  int d = 0; // NOSONAR
  int d = 0; // NOPMD
  int d = 0; // CHECKSTYLE:OFF
  int d = 0; // checkstyle:off
  int d = 0; // checkstyle:off explanation

  void foo() {
    int[] m = new int[2];
    // Noncompliant@+1
    if (i == 0) { // This is non-compliant
      m[0] = 1;
    }
  }
}
