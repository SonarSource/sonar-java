class PreTest<T> {
  <S extends T> void foo(S s) {}
}

interface PreInterface<X> {
  default <Y extends X> void bar(Y y) {}
}

class Test<U> extends PreTest<U> implements PreInterface<U> {
  void test() {
    new Test<A>().<A>foo(new A());
    new Test<A>().<B>foo(new B());
    new Test<A>().foo(new A());
    new Test<A>().foo(new B());

    new Test<A>().<A>bar(new A());
    new Test<A>().<B>bar(new B());
    new Test<A>().bar(new A());
    new Test<A>().bar(new B());
  }
}

interface Interface<Z> extends PreInterface<Z> {}
class Test2 extends Test<A> implements Interface<A> {
  void test2() {
    new Test2().<A>foo(new A());
    new Test2().<B>foo(new B());
    new Test2().foo(new A());
    new Test2().foo(new B());

    new Test2().<A>bar(new A());
    new Test2().<B>bar(new B());
    new Test2().bar(new A());
    new Test2().bar(new B());
  }
}

interface Interface2 extends Interface<A> {}
class Test3 extends Test2 implements Interface2 {
  void test3() {
    new Test3().<A>foo(new A());
    new Test3().<B>foo(new B());
    new Test3().foo(new A());
    new Test3().foo(new B());

    new Test3().<A>bar(new A());
    new Test3().<B>bar(new B());
    new Test3().bar(new A());
    new Test3().bar(new B());
  }
}

class A {}
class B extends A {}
