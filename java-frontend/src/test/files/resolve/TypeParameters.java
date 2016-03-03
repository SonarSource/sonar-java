package org.foo;

class TypeParameters {

  <T> B<T> f1(T t) {
    return null;
  }

  <T> T f2(B<T> b) {
    return null;
  }

  <T> T f3(int i, B<T> b) {
    return null;
  }

  <T> T[] f4(T[] a) {
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

  void test_resolution(B<?> bwc, B<A> ba, B<? extends A> bwcEa) {
    f1("String");
    f2(new B<Integer>());
    f2(new B());
    f3(0, new B<Integer>());
    f4(new String[0]);
    f5("foo", Integer.valueOf(42));
    f5("foo", 42);
    f6(ba);
    f6(bwc);
    f6(bwcEa);
    f7(new B<B<Integer>>());
    f8(new A());
    f8(new B<String>());
    f9(new A());
    f9(new B());
    f9(new D());
    f10(42);
  }

  // reference types
  Object object;
  B<String> bString;
  Integer integer;
  String[] stringArray;
  C<String, Integer> cStringInteger;
  B<? super A> wcSuperA;
  B<? super Object> wcSuperObject;
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
