class PreTest<T> {
  <S extends T> void foo(S s) {}
}

class Test<T> extends PreTest<T> {
  void test() {
    new Test<A>().<A>foo(new A());
    new Test<A>().<B>foo(new B());
    new Test<A>().foo(new A());
    new Test<A>().foo(new B());
  }
}

class Test2 extends Test<A> {
  void test2() {
    new Test2().<A>foo(new A());
    new Test2().<B>foo(new B());
    new Test2().foo(new A());
    new Test2().foo(new B());
  }
}

class Test3 extends Test2 {
  void test3() {
    new Test3().<A>foo(new A());
    new Test3().<B>foo(new B());
    new Test3().foo(new A());
    new Test3().foo(new B());
  }
}

class A {}
class B extends A {}
