class A<V> implements I<V> {
  A() {}
  A(V v) {}
  A(A<? extends V> a) {}

  V value;
  A<String> aString;
  A<Object> aObject;

  void foo(V v) {}
  static void bar(A<String> a) {}

  A<String> method(Object o) {
    new A<String>();

    new A<>();
    new A<>().foo(o);
    if (new A<>().value != null) {}

    I<String> var = new A<>();
    I var2 = new A<>();

    bar(new A<>()); // compile with java 8, but not 7

    new A<>(o);
    new A<>("test");

    new A<>(aString);

    return new A<>();
  }
}

interface I<X> {}
interface J<Y, Z> {}
class B<X, Y, Z> implements I<X>, J<Y, Z> {
  B() {}

  J<String, Integer> j = new B<>();

  void method() {
    qix(new B<>());
    bar(new B<>());
  }

  static void qix(I<String> a) {}
  static void bar(J<String, Integer> a) {}
}

class C {}
interface K {}
class D<X extends C> {
  D() {}

  void foo(X x) {}

  D<C> dC;

  void method() {
    new D<>().foo(null);
  }
}

class E {
  class F<X> {
    F() {}
    F(int i, X x, C c) {}
  }

  F<String> fString;

  void foo(F<String> f) {}

  void method(E e) {
    foo(e.new F<String>());
    foo(e.new F<>(42, "test", new C()));
  }
}
