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
    int k; int l; int m, n; // Noncompliant [[sc=16;ec=17;secondary=+0,+0;quickfixes=qf_mixed1]]
    // fix@qf_mixed1 {{Declare on separated lines}}
    // edit@qf_mixed1 [[sc=11;ec=12]] {{\n    }}
    // edit@qf_mixed1 [[sc=18;ec=19]] {{\n    }}
    // edit@qf_mixed1 [[sc=24;ec=26]] {{;\n    int }}

    int k1; int l1, m1; int n1; // Noncompliant [[sc=17;ec=19;secondary=+0,+0;quickfixes=qf_mixed2]]
    // fix@qf_mixed2 {{Declare on separated lines}}
    // edit@qf_mixed2 [[sc=12;ec=13]] {{\n    }}
    // edit@qf_mixed2 [[sc=19;ec=21]] {{;\n    int }}
    // edit@qf_mixed2 [[sc=24;ec=25]] {{\n    }}
  }

  class TestQuickFix {
    int i; int j; // Noncompliant [[sc=16;ec=17;quickfixes=qf1]]
    // fix@qf1 {{Declare on separated lines}}
    // edit@qf1 [[sc=11;ec=12]] {{\n    }}

    int i2; int j2;     int k2;    int l2;// Noncompliant [[sc=17;ec=19;quickfixes=qf2]]
    // fix@qf2 {{Declare on separated lines}}
    // edit@qf2 [[sc=12;ec=13]] {{\n    }}
    // edit@qf2 [[sc=20;ec=25]] {{\n    }}
    // edit@qf2 [[sc=32;ec=36]] {{\n    }}

    int i3, j3; // Noncompliant [[sc=13;ec=15;quickfixes=qf3]]
    // fix@qf3 {{Declare on separated lines}}
    // edit@qf3 [[sc=11;ec=13]] {{;\n    int }}

    int i4, j4, k4, l4; // Noncompliant [[sc=13;ec=15;quickfixes=qf4]]
    // fix@qf4 {{Declare on separated lines}}
    // edit@qf4 [[sc=11;ec=13]] {{;\n    int }}
    // edit@qf4 [[sc=15;ec=17]] {{;\n    int }}
    // edit@qf4 [[sc=19;ec=21]] {{;\n    int }}

    final int i5 = 1, j5 = 1; // Noncompliant [[sc=23;ec=25;quickfixes=qf5]]
    // fix@qf5 {{Declare on separated lines}}
    // edit@qf5 [[sc=21;ec=23]] {{;\n    final int }}

    int i6,

    j6, // Noncompliant [[sc=5;ec=7;quickfixes=qf6]]


       k6;
    // fix@qf6 {{Declare on separated lines}}
    // edit@qf6 [[sl=-2;sc=11;el=+0;ec=5]] {{;\n    int }}
    // edit@qf6 [[sl=+0;sc=7;el=+3;ec=8]] {{;\n    int }}

    void m() {
      int i7; m(); int j7; // Noncompliant [[sc=24;ec=26;quickfixes=qf17]]
      // fix@qf17 {{Declare on separated lines}}
      // edit@qf17 [[sc=19;ec=20]] {{\n      }}
    }
  }

  int correct; int indentation; // Noncompliant [[sc=20;ec=31;quickfixes=qf_indentation]]
  // fix@qf_indentation {{Declare on separated lines}}
  // edit@qf_indentation [[sc=15;ec=16]] {{\n  }}

int no; int spaceBefore; // Noncompliant [[sc=13;ec=24;quickfixes=qf_indentation2]]
  // fix@qf_indentation2 {{Declare on separated lines}}
  // edit@qf_indentation2 [[sc=8;ec=9]] {{\n}}

  // Noncompliant@+1 {{Declare "i8" on a separate line.}}
  int i7; int i8;
  int i9,
      i10;  // Noncompliant {{Declare "i10" on a separate line.}}
  // For corner case test, last variable must be a "dubble" declaration with no simple declaration after ; and end token on next line.
  // Noncompliant@+1 {{Declare "i12" on a separate line.}}
  int i11; int i12
  ;
}
