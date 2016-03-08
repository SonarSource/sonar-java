package org.foo;

// JLS8 5.1.9 + JLS8 15.12.2.6
class B<E> {
  <T> T[] foo(T[] t) {
    return null;
  }

  <T> T bar(T t) {
    return null;
  }

  <T> B<T> qix(T t) {
    return null;
  }

  <T> B<B<T>> gul(T t) {
    return null;
  }

  <T extends A> T lot(T t) {
    return null;
  }

  // unchecked conversion
  void tst1(B b, B<String> bString, A[] array, A a, C c) {
    // return type is 'Object[]' instead of 'A[]'
    objectArrayType = b.foo(array);
    // return type is 'Object' instead 'A'
    objectType = b.bar(a);
    // return type is 'B' instead 'B<A>'
    bRawType = b.qix(a);
    // return type is 'B' instead 'B<B<T>>'
    bRawType = b.gul(a);
    aType = b.lot(c);
  }

  void tst2(B<String> b, A[] array, A a, C c) {
    aArrayType = b.foo(array);
    aType = b.bar(a);
    bAType = b.qix(a);
    bBAType = b.gul(a);
    cType = b.lot(c);

    aArrayType = this.<A>foo(array);
    aType = this.<A>bar(a);
    bAType = this.<A>qix(a);
    bBAType = this.<A>gul(a);
    cType = this.<C>lot(c);
  }

  Object[] objectArrayType;
  Object objectType;
  B bRawType;
  B<A> bAType;
  B<B<A>> bBAType;
  A aType;
  A[] aArrayType;
  C cType;
}

class A {
}

class C extends A {
}
