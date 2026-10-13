class A {
  int a = 0;
  int b = 1;
  int c = 42;
  int d = 010; // Noncompliant {{Use decimal values instead of octal ones. If an octal value is intended, use the more explicit "0_" prefix.}}
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

  int perm1 = 0400; // Noncompliant {{Use the symbolic permission string "r--------" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
//            ^^^^
  int perm2 = 0444; // Noncompliant {{Use the symbolic permission string "r--r--r--" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm3 = 0600; // Noncompliant {{Use the symbolic permission string "rw-------" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm4 = 0640; // Noncompliant {{Use the symbolic permission string "rw-r-----" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm5 = 0644; // Noncompliant {{Use the symbolic permission string "rw-r--r--" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm6 = 0660; // Noncompliant {{Use the symbolic permission string "rw-rw----" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm7 = 0664; // Noncompliant {{Use the symbolic permission string "rw-rw-r--" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm8 = 0666; // Noncompliant {{Use the symbolic permission string "rw-rw-rw-" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm9 = 0700; // Noncompliant {{Use the symbolic permission string "rwx------" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm10 = 0750; // Noncompliant {{Use the symbolic permission string "rwxr-x---" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm11 = 0755; // Noncompliant {{Use the symbolic permission string "rwxr-xr-x" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm12 = 0770; // Noncompliant {{Use the symbolic permission string "rwxrwx---" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm13 = 0775; // Noncompliant {{Use the symbolic permission string "rwxrwxr-x" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm14 = 0777; // Noncompliant {{Use the symbolic permission string "rwxrwxrwx" instead of this octal value. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm15 = 01777; // Noncompliant {{Use decimal values instead of octal ones. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm16 = 02755; // Noncompliant {{Use decimal values instead of octal ones. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm17 = 04755; // Noncompliant {{Use decimal values instead of octal ones. If an octal value is intended, use the more explicit "0_" prefix.}}
  int perm18 = 0_755;
  int perm19 = 0_644;

  int explicit1 = 0_7;
  int explicit2 = 0_13;
  int explicit3 = 0_123;
  int explicit4 = 0_311;
  int explicit5 = 0_1234;
  int explicit6 = 0_12_34;
  int underscore1 = 01_234; // Noncompliant
  int underscore2 = 00_13; // Noncompliant
}
