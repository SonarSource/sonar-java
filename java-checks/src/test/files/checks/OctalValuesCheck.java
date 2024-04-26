class A {
  int a = 0;
  int b = 1;
  int c = 42;
  int d = 010; // Noncompliant {{Use decimal values instead of octal ones.}}
//        ^^^
  int e = 00; // Noncompliant
  int f = 0.;
  int g = 0x00;
  int h = 0X00;
  int j = 0b0101;
  int k = 0B0101;
}
