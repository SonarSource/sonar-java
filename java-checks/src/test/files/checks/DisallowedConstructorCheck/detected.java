class A {
  A(int i, long l, String[] s) {
  }
  A(){}
  class B {
    B(int i, long l, String[] s) {
    }

    B() {
    }
  }
  void plop() {
   new A(1,2,new String[]{"1"}); // Noncompliant [[sc=8;ec=9]] {{Remove this forbidden initialization}}
   new A();
   new B(1,2,new String[]{"1"});
   new B();
  }
}
