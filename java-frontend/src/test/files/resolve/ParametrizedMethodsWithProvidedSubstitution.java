package org.foo;

class ParametrizedMethodsWithProvidedSubstitution {

  <T> B<T> f1(T t) {
    return null;
  }

  A f1(A a) {
    return null;
  }

  <T> T f2(B<T> bt) {
    return null;
  }

  A f2(A a) {
    return null;
  }

  <T> T f3(int i, B<T> b) {
    return null;
  }

  <T> T[] f4(T[] a) {
    return null;
  }

  <T> T[][] f4(T[] a, int i) {
    return null;
  }

  <K, V> C<K, V> f5(K k, V v) {
    return null;
  }

  <T> B<? super T> f6(B<? extends T> c) {
    return null;
  }

  <T> T f7(B<B<T>> b) {
    return null;
  }

  Object f8(Object o) {
    return null;
  }

  <T extends B> T f8(T t) {
    return null;
  }

  Object f9(Object o) {
    return null;
  }

  <T extends B & I> T f9(T t) {
    return null;
  }

  <T> T f10(T... a) {
    return null;
  }

  void test_resolution() {
    this.<String>f1("String");
    this.<B>f1(new B());
    this.<A>f1(new A());

    this.<Integer>f2(new B<Integer>());
    this.<String>f2(new D());
    this.<A>f2(new B());
    this.<A>f2(new A());
    
    this.<Integer>f3(0, new B<Integer>());

    this.<String>f4(new String[0]);
    this.<String>f4(new String[0], 42);

    this.<String, Integer>f5("foo", Integer.valueOf(42));
    this.<String, Integer>f5("foo", 42);
    this.<A, B>f5(new A(), new B());

    this.<A>f6(new B<A>());

    this.<Integer>f7(new B<B<Integer>>());

    this.<A>f8(new A());
    this.<B>f8(new B<String>());
    this.<D>f8(new D());

    this.<A>f9(new A());
    this.<B>f9(new B());
    this.<D>f9(new D());

    this.<Integer>f10(42);
  }

  // reference types
  Object object;
  Integer integer;
  String string;
  String[] stringArray;

  A aType;
  B bType;
  B<B> bb;
  B<String> bString;
  B<Integer> bInteger;
  B<? super A> wcSuperA;
  C<A, B> cAB;
  C<String, Integer> cStringInteger;
  D dType;

  interface I {
  }

  class A {
  }

  class B<X> {
  }

  class C<X, Y> {
  }

  class D extends B<String> implements I {
  }
}
