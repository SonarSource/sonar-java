package org.foo;

class ParametrizedMethodsWithTypeInference {

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

  <U, V> C<V, U> f11(V v, B<U> b) {
    return null;
  }

  <T> T f12(B<? extends Number> b1, B<? extends T> b2) {
    return null;
  }

  <T> T f13(T[][][][] a) {
    return null;
  }

  <T> B<T> f14(T... a) {
    return null;
  }

  <T, U> T f15(U u, T... t) {
    return null;
  }

  <T> B<T> f16(B<? extends T> ... b) {
    return null;
  }

  <X, T, V extends T> Object f17(C<X, T> c, B<V> b) {
    return f17(c, b);
  }

  void test_resolution(B<?> bwc, B<A> ba, B<? extends A> bwcEa) {
    f1("String");
    f1(null);
    f2(new B<Integer>());
    f2(new B());
    f2(null);
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
    f10(1.0, 42);
    f10(new E(), new F(), new G());
    f11("hello", ba);
    f12(new B<Integer>(), ba);
    f13(new Integer[0][0][0][0]);
    f14("hello", "world");
    f15(new A());

    f16(new B());
    f16(new B(), new B());
    f16(new B(), new B<Integer>());
    f16(new B<String>(), new B<Integer>());
    f16(new B<String>(), new D());
  }

  // reference types
  Object object;
  B<String> bString;
  B<Object> bObject;
  Integer integer;
  Number number;
  String[] stringArray;
  C<String, Integer> cStringInteger;
  C<String, A> cStringA;
  B<? super A> wcSuperA;
  B<? super Object> wcSuperObject;
  D dType;
  A aType;
  B<Comparable<? extends Comparable>> comparable;
  B bRawType;

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

  class E extends A {}
  class F extends E {}
  class G extends A {}
}
