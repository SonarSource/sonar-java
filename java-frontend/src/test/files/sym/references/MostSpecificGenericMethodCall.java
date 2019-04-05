package org.foo;

abstract class A {

  void tst(String[] p1, B<String> p2, boolean p3) {
    foo(p1);
    foo(p2);
    foo(p3);
  }

  abstract <T> T foo(T t);
  abstract <T> T[] foo(T[] t);
  abstract <T> B<T> foo(B<? extends T> t);

  static class B<X> { }
}
