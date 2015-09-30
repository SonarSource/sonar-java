class Foo {
  int i = 0,
    j = 0, // Noncompliant {{Move this trailing comment on the previous empty line.}}
    k = 0;

  // This is compliant
  int a = 0; //    Compliant
  int b = 0; /*    Compliant   */
  int c = 0; // Noncompliant {{Move this trailing comment on the previous empty line.}}
  int d = 0; // Noncompliant This is also non-compliant

  int e = /* Compliant */ 0;
  int d = 0; // NOSONAR
  int d = 0; // NOPMD
  int d = 0; // CHECKSTYLE:OFF
  int d = 0; // checkstyle:off
  int d = 0; // checkstyle:off explanation

  void foo() {
    int[] m = new int[2];
    if (i == 0) { // Noncompliant This is non-compliant
      m[0] = 1;
    }
  }
}
