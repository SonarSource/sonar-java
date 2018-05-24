class a {
  byte b = 0; // Noncompliant {{Remove this initialization to "0", the compiler will do that for you.}}
  byte b2;
  short s = 0; // Noncompliant {{Remove this initialization to "0", the compiler will do that for you.}}
  short s1 = 1;
  short s2;
  int i = 0; // Noncompliant {{Remove this initialization to "0", the compiler will do that for you.}}
  int i1 = 1;
  int i2;
  final int i3 = 0;
  private static final long serialVersionUID = 0;
  long l = 0; // Noncompliant {{Remove this initialization to "0", the compiler will do that for you.}}
  long l1 = (0L); // Noncompliant {{Remove this initialization to "0L", the compiler will do that for you.}}
  long l2 = 0xFFFF_FFFF_FFFF_FFFFL;
  long l3;
  long l4 = 0x0000_0000_0000_0000L; // Noncompliant {{Remove this initialization to "0x0000_0000_0000_0000L", the compiler will do that for you.}}
  long l5 = 0b00000000_00000000_00000000_00000000; // Noncompliant {{Remove this initialization to "0b00000000_00000000_00000000_00000000", the compiler will do that for you.}}
  float f = 0; // Noncompliant {{Remove this initialization to "0", the compiler will do that for you.}}
  float f1 = 0.f; // Noncompliant {{Remove this initialization to "0.f", the compiler will do that for you.}}
  float f2 = 1.f;
  float f3;
  double d = 0.; // Noncompliant {{Remove this initialization to "0.", the compiler will do that for you.}}
  double d1 = 1.;
  double d2;
  double d3 = 0.00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001; // Compliant, not 0
  char c = 0; // Noncompliant {{Remove this initialization to "0", the compiler will do that for you.}}
  char c1 = '\u0000'; // Noncompliant [[sc=13;ec=21]] {{Remove this initialization to "'\u0000'", the compiler will do that for you.}}
  char c2 = '0';
  char c3;
  char ch4 = '\0'; // Noncompliant {{Remove this initialization to "'\0'", the compiler will do that for you.}}
  boolean bool = false; // Noncompliant {{Remove this initialization to "false", the compiler will do that for you.}}
  boolean bool1 = true;
  boolean bool2;
  Object o = null; // Noncompliant {{Remove this initialization to "null", the compiler will do that for you.}}
  Object o2;
  String str = null; // Noncompliant {{Remove this initialization to "null", the compiler will do that for you.}}
  String str1 = "a";
  String str2;
  Integer int1 = null;  // Noncompliant {{Remove this initialization to "null", the compiler will do that for you.}}
  Integer int2 = 0;

  void method() {}

  class ExplicitInit {
      private Bar<String> bar = null; // Noncompliant {{Remove this initialization to "null", the compiler will do that for you.}}
      private Bar<String>[] barArray = null; // Noncompliant {{Remove this initialization to "null", the compiler will do that for you.}}
  }

  static final long staticInit;

  static {
    staticInit = 0; // Compliant, post init for a final field
  }
}
