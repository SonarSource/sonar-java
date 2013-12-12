class Foo {
  // This is compliant
  int a = 0; //    Compliant
  int b = 0; /*    Compliant   */
  int c = 0; // This is non-compliant
  int d = 0; // This is also non-compliant

  int e = /* Compliant */ 0;
  int d = 0; // NOSONAR
  int d = 0; // NOPMD
  int d = 0; // CHECKSTYLE:OFF
  int d = 0; // checkstyle:off
  int d = 0; // checkstyle:off explanation
}
