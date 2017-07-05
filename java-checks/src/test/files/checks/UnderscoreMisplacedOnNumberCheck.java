class A {

  int i0 = 1000; // Compliant
  int i1 = 10000000; // compliant
  int i2 = 10_000_000; // Compliant
  int i3 = 10_000_00_000; // Noncompliant {{Review this number; its irregular pattern indicates an error.}}

  int b0 = 0b0000; // Compliant
  int b1 = 0b01101001010011011110010101011110; // compliant
  int b2 = 0b0110_1001_0100_1101_1110_0101_0101_1110; // compliant
  int b3 = 0b011010010100110_111_10010101011110; // Noncompliant {{Review this number; its irregular pattern indicates an error.}}
  int b4 = 10000_000_000; // Noncompliant

  long l0 = 0x0000L; // Compliant
  long l1 = 0x7fffffffffffffffL; // compliant
  long l2 = 0x7fff_ffff_ffff_ffffL; // compliant
  long l3 = 0X7fff_ffff_ffff_ffffl; // compliant
  long l4 = 0x7fff_fff_ffff_ffffL; // Noncompliant {{Review this number; its irregular pattern indicates an error.}}

}
