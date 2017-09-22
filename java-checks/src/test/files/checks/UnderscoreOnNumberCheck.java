class A {
  private static final long serialVersionUID = -8778479691651900590L;
  int i0 = 1000; // Compliant
  int i1 = 10000; // Compliant
  int i2 = 100000; // Noncompliant {{Add underscores to this numeric value for readability}}
  int i3 = 10000000; // Noncompliant
  int i4 = 10_000_000; // Compliant

  int b0 = 0b0000; // Compliant
  int b1 = 0b01101001010011011110010101011110; // Noncompliant
  int b2 = 0b011010010100110_111_10010101011110; // Compliant - I put underscore where I want!

  long l0 = 0x0000L; // Compliant
  long l1 = 0x7fffffffffffffffL; // Noncompliant
  long l2 = 0X7FFFFFFFFFFFFFFFL; // Noncompliant
  long l3 = 0x7fff_ffff_ffff_ffffL; // Compliant

  long octal0 = 012354435; // Compliant
  long octal1 = 012354435242; // Noncompliant

  void foo(int x) {
    if (x < -8778479691651900590L) {} // Noncompliant
  }
}
