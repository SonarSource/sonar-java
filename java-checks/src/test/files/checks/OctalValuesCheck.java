class A {
  int a = 0;
  int b = 1;
  int c = 42;
  int d = 010; // Noncompliant {{Use decimal values instead of octal ones.}}
//        ^^^
  int e = 00;
  int f = 0.;
  int g = 0x00;
  int h = 0X00;
  int j = 0b0101;
  int k = 0B0101;
  int l = 01;
  int m = 02;
  int n = 07;
  int o = 012; // Noncompliant
  int p = 0123; // Noncompliant
  int q = 0012; // Noncompliant
  int r = 0311; // Noncompliant
  int s = 01234; // Noncompliant

  int perm1 = 0400;
  int perm2 = 0444;
  int perm3 = 0600;
  int perm4 = 0640;
  int perm5 = 0644;
  int perm6 = 0660;
  int perm7 = 0664;
  int perm8 = 0666;
  int perm9 = 0700;
  int perm10 = 0750;
  int perm11 = 0755;
  int perm12 = 0770;
  int perm13 = 0775;
  int perm14 = 0777;
  int perm15 = 01777;
  int perm16 = 02755;
  int perm17 = 04755;
}
