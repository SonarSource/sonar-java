package checks;

class SelfAssignementCheck {
  int a,c = 0;
  int[] b = {0};
  void method() {
    a = a; // Noncompliant [[sc=7;ec=8]] {{Remove or correct this useless self-assignment.}}
    this.a = this.a; // Noncompliant
    this.a = a; // Noncompliant [[sc=12;ec=13]] {{Remove or correct this useless self-assignment.}}
    b[0] = b[0]; // Noncompliant
    a = c = c; // Noncompliant
    b[fun()] = b[fun()]; // Noncompliant
  }
  void method2(SelfAssignementCheck c, int a) {
    this.a = c.a;
    this.a = a;
  }

  int fun(){
    return 0;
  }
}

class SelfAssignementCheckB {
  static int b;
  int foo;
  class SelfAssignementCheckC {
    void fun() {
      SelfAssignementCheckB.b = b; // Noncompliant
    }
  }
  void setFoo(int foo){
    this.foo = foo;
  }
  SelfAssignementCheckB(SelfAssignementCheckB bInstance) {
    foo = bInstance.foo;
  }
}
