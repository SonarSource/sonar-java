class A {
  int a,c = 0;
  int[] b = {0};
  void method() {
    a = a;
    this.a = this.a;
    this.a = a; //False negative
    b[0] = b[0];
    a = c = c;
    b[fun()] = b[fun()];
  }

  int fun(){
    return 0;
  }
}

class B{
  static int b;
  int foo;
  class C {
    void fun() {
      B.b = b;
    }
  }
  void setFoo(int foo){
    this.foo = foo;
  }
  B(B bInstance) {
    foo = bInstance.foo; //False negative
  }
}