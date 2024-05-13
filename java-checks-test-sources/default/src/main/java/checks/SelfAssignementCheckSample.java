package checks;

class SelfAssignementCheckSample {
  static int staticField = 0;
  
  static {
    staticField = staticField; // Noncompliant
  }
  
  int a, c = 0;
  int[] b = {0};

  int m = a = a; // Noncompliant [[quickfixes=!]]
//          ^

  int x = a = a = a; // Noncompliant [[quickfixes=!]]
//              ^

  int s = getS(); // Compliant

  int getS() {
    return 0;
  }

  void method() {
    a = a; // Noncompliant {{Remove or correct this useless self-assignment.}}
//    ^
    this.a = this.a; // Noncompliant
    this.a = a; // Noncompliant {{Remove or correct this useless self-assignment.}}
//         ^
    b[0] = b[0]; // Noncompliant
    a = c = c; // Noncompliant
    b[fun()] = b[fun()]; // Noncompliant
  }

  void method2(SelfAssignementCheckSample c, int a) {
    this.a = c.a;
    this.a = a;
  }

  int fun() {
    return 0;
  }
}

class SelfAssignementCheckSampleB {
  static int b;
  int foo;

  class SelfAssignementCheckSampleC {
    void fun() {
      SelfAssignementCheckSampleB.b = b; // Noncompliant
    }
  }

  void setFoo(int foo) {
    this.foo = foo;
  }

  SelfAssignementCheckSampleB(SelfAssignementCheckSampleB bInstance) {
    foo = bInstance.foo;
  }
}

class SelfAssignmentCheckC {

  String a;

  public SelfAssignmentCheckC(String a) {
    a = a; // Noncompliant {{Remove or correct this useless self-assignment.}} [[quickfixes=qf1]]
//    ^
    // fix@qf1 {{Disambiguate this self-assignment}}
    // edit@qf1 [[sc=5;ec=5]] {{this.}}
  }
}

class SelfAssignmentCheckD {

  String a;
  String b;
  String c;

  public SelfAssignmentCheckD(String a, String b, String d) {
    a = a; // Noncompliant {{Remove or correct this useless self-assignment.}} [[quickfixes=qf2]]
//    ^
    // fix@qf2 {{Disambiguate this self-assignment}}
    // edit@qf2 [[sc=5;ec=5]] {{this.}}

    c = c; // Noncompliant {{Remove or correct this useless self-assignment.}} [[quickfixes=qf3]]
//    ^
    // fix@qf3 {{Remove this useless self-assignment}}
    // edit@qf3 [[sc=5;ec=11]] {{}}

    this.c = c; // Noncompliant {{Remove or correct this useless self-assignment.}} [[quickfixes=qf4]]
//         ^
    // fix@qf4 {{Remove this useless self-assignment}}
    // edit@qf4 [[sc=5;ec=16]] {{}}

    d = d; // Noncompliant {{Remove or correct this useless self-assignment.}} [[quickfixes=qf5]]
//    ^
    // fix@qf5 {{Remove this useless self-assignment}}
    // edit@qf5 [[sc=5;ec=11]] {{}}

    this.a = getA(); // Compliant

    this.b = b; // Compliant
    this.c = b; // Compliant
  }

  String getA() {
    return "";
  }
}

interface SelfAssignmentCheckE {

  int c = 3;
  int a = c; // Compliant
  default void method() {
    int b = 3;
    int m = b; // Compliant
    b = b; // Noncompliant
  }

  default void method1(int b) {
    int a;
    b = b; // Noncompliant
    a = b; // Compliant
  }
}
