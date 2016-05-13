class A {
  class InnerA {
    InnerA(int i) { }
  }
}

class B {
  class InnerB extends A.InnerA {
    InnerB(A a, int i) {
      a.super(i);
    }
  }
}
