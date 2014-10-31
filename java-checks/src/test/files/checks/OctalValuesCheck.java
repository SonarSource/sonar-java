class A {
  int a = 0; // Compliant
  int b = 1; // Compliant
  int c = 42; // Compliant
  int d = 010; // Noncompliant
  int e = 00; // Noncompliant
  int f = 0.; // Compliant
  int g = 0x00; // Compliant
  int h = 0X00; // Compliant
  int j = 0b0101; // Compliant
  int k = 0B0101; // Compliant
}
