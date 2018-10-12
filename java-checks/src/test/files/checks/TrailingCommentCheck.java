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
  int f = 0; // NOSONAR
  int g = 0; // NOPMD
  int h = 0; // CHECKSTYLE:OFF
  int l = 0; // checkstyle:off
  int m = 0; // checkstyle:off explanation
  String s = "Hello" + "World"; //$NON-NLS-1$ //$NON-NLS-2$

  void foo() {
    int[] m = new int[2];
    if (i == 0) { // Noncompliant This is non-compliant
      m[0] = 1;
    }
  }
}
