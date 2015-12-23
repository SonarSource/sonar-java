class A {
  int a,c = 0;
  int[] b = {0};
  void method() {
    a = a; // Noncompliant [[sc=7;ec=8]] {{Remove or correct this useless self-assignment.}}
    this.a = this.a; // Noncompliant
    this.a = a; // false negative
    b[0] = b[0]; // Noncompliant
    a = c = c; // Noncompliant
    b[fun()] = b[fun()]; // Noncompliant
  }
  void method2(A c, int a) {
    this.a = c.a;
    this.a = a;
  }

  int fun(){
    return 0;
  }
}

class B {
  static int b;
  int foo;
  class C {
    void fun() {
      B.b = b; // false negative
    }
  }
  void setFoo(int foo){
    this.foo = foo;
  }
  B(B bInstance) {
    foo = bInstance.foo;
  }
}
