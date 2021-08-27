package checks;

import java.util.Collections;

class OneDeclarationPerLineCheck {

  interface MyInterface {
    int a = 1, b = 1; // Noncompliant [[sc=16;ec=17]] {{Declare "b" on a separate line.}}
  }

  enum MyEnum {
    ONE, TWO;
    int a, b; // Noncompliant
  }

  @interface annotationType {
    int a = 0, b = 0; // Noncompliant
  }

  private static final int STATIC_FINAL_A = 1, STATIC_FINAL_B = 1; // Noncompliant {{Declare "STATIC_FINAL_B" on a separate line.}}

  static {
    int staticA, staticB = 1;     // Noncompliant {{Declare "staticB" on a separate line.}}
    int staticC = 1; int staticD; // Noncompliant {{Declare "staticD" on a separate line.}}
  }

  int a; int b; int c; int d; // Noncompliant [[sc=14;ec=15;secondary=+0,+0]] {{Declare "b" and all following declarations on a separate line.}}
  int e;

  private int j1 = -1, j2 // Noncompliant [[sc=24;ec=26;secondary=+2,+4]] {{Declare "j2" and all following declarations on a separate line.}}

    , j3 = 1

    , j4;

  private int i1 = -1, i2, i3 = 1; // Noncompliant [[sc=24;ec=26;secondary=+0]] {{Declare "i2" and all following declarations on a separate line.}}

  ;

  // For corner case test, please add a space before ;
  Object o1, o2 ; // Noncompliant {{Declare "o2" on a separate line.}}

  private String s1; // Compliant, only one on the line

  OneDeclarationPerLineCheck(){
    int const1,const2; // Noncompliant {{Declare "const2" on a separate line.}}
  }

  public void method() {
    int i4 = 0, i5 = -1; // Noncompliant {{Declare "i5" on a separate line.}}
    int i6 = 1; // Compliant, only one on the line

    for (int i = 0, j = 0; i < 1; i++) { // Compliant in statment declaration
      int forA, forB; // Noncompliant {{Declare "forB" on a separate line.}}
      forA = i;
      forB = j;
      int forAfterA = forA, forAfterB; // Noncompliant {{Declare "forAfterB" on a separate line.}}
      int forAfterC; int forAfterD = forB; // Noncompliant {{Declare "forAfterD" on a separate line.}}
    }
  }

  public void cornerCase(int a, int b) {  // should not raise an issue
    try {
    } catch(Exception e) { // NPE verify (variable w/o endToken)
    }

    for (Object o : Collections.emptyList()) { // NPE verify (variable w/o endToken)
    }


    switch (2) {
      case 1:
        int a1, b1 = 0; // Noncompliant
        break;
      case 2:
        break;
    }
  }
  class reportedAfterMethod {
    int i; int j; // Noncompliant
    void f() {}
    int k; int l; // Noncompliant
  }

  void mixedAreReportedTogether() {
    int k; int l; int m, n; // Noncompliant [[secondary=+0,+0]]
    int k1; int l1, m1; int n1; // Noncompliant [[secondary=+0,+0]]
  }

  // Noncompliant@+1 {{Declare "i8" on a separate line.}}
  int i7; int i8;
  int i9,
      i10;  // Noncompliant {{Declare "i10" on a separate line.}}
  // For corner case test, last variable must be a "dubble" declaration with no simple declaration after ; and end token on next line.
  // Noncompliant@+1 {{Declare "i12" on a separate line.}}
  int i11; int i12
  ;
}
