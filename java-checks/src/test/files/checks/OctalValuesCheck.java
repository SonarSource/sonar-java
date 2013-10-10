class A {
  int a = 0; // Compliant
  int b = 1; // Compliant
  int c = 42; // Compliant
  int d = 010; // Noncompliant
  int e = 00; // Noncompliant
  int f = 0.; // Compliant
}
