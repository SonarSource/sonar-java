interface MyInterface {
  int a = 1, b = 1; // Noncompliant {{Declare "b" on a separate line.}}
}

enum MyEnum {
  ONE, TWO;
  int a, b; // Noncompliant
}

@interface annotationType {
  int a = 0, b = 0; // Noncompliant
}

class OneDeclarationPerLineCheck {

  private static final int STATIC_FINAL_A = 1, STATIC_FINAL_B = 1; // Noncompliant {{Declare "STATIC_FINAL_B" on a separate line.}}

  static {
    int staticA, staticB = 1;     // Noncompliant {{Declare "staticB" on a separate line.}}
    int staticC = 1; int staticD; // Noncompliant {{Declare "staticD" on a separate line.}}
  }

  private int i1 = -1, i2, i3 = 1; // Noncompliant
                                   // Noncompliant@-1
                                   // Issue order not stable => no message test below
  
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

    for (Object o : Lists.newLinkedList()) { // NPE verify (variable w/o endToken)
    }


    switch (2) {
      case 1:
        int a, b = 0; // Noncompliant
        break;
      case 2:
        break;
    }
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
