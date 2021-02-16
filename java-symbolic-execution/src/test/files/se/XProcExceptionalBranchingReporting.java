package org.foo.bar;

import javax.annotation.CheckForNull;

abstract class A {

  private void foo(boolean b, Object o) throws MyException1 {
    if (b // flow@catof2 [[order=2]] {{Implies 'b' is true.}}
      && o == null) { // flow@catof1 [[order=2]] {{Implies 'o' can be null.}} flow@npe [[order=2]] {{Implies 'o' can be null.}}
      throw new MyException1();
    }
  }

  void tst(Object o, boolean b) {
    try {
      foo(b, o); // flow@catof1 [[order=1]] {{'o' is passed to 'foo()'.}} flow@catof1 [[order=3]] {{Implies 'o' is null.}} flow@catof1 [[order=4]] {{'MyException1' is thrown.}} flow@npe [[order=1]] {{'o' is passed to 'foo()'.}} flow@npe [[order=3]] {{Implies 'o' is null.}}  flow@npe [[order=4]] {{'MyException1' is thrown.}} flow@catof2 [[order=1]] {{'b' is passed to 'foo()'.}}  flow@catof2 [[order=3]] {{Implies 'b' is true.}} flow@catof2 [[order=4]] {{'MyException1' is thrown.}}
    } catch (MyException1 e) { // flow@npe,catof1,catof2 [[order=5]] {{'MyException1' is caught.}}
      if (b) { // Noncompliant [[flows=catof2]] {{Remove this expression which always evaluates to "true"}} flow@catof2 [[order=6]] {{Expression is always true.}}
        if (o == null) {} // Noncompliant [[flows=catof1]] {{Remove this expression which always evaluates to "true"}} flow@catof1 [[order=6]] {{Expression is always true.}}
      }
    } finally {
      o.toString(); // Noncompliant [[flows=npe]] {{A "NullPointerException" could be thrown; "o" is nullable here.}}  flow@npe [[order=6]] {{'o' is dereferenced.}}
    }
  }

  @CheckForNull
  abstract void bar(Object o);

  void tst1(Object o) {
    Object o2 = bar(o); // flow@npe1 {{'bar()' can return null.}} flow@npe1 {{Implies 'o2' can be null.}}
    o2.toString(); // Noncompliant [[flows=npe1]] {{A "NullPointerException" could be thrown; "o2" is nullable here.}} flow@npe1 {{'o2' is dereferenced.}}
  }

  private Object returnParam(Object o) {
    return o;
  }

  void tst2(Object o) {
    if (o == null) { // flow@npe2 {{Implies 'o' can be null.}}
      // FIXME "'o' is passed to 'returnParam'" message is missing because there is no flow produced in returnParam method
      Object o2 = returnParam(o); // flow@npe2 {{Implies 'o2' can be null.}}
      o2.toString(); // Noncompliant [[flows=npe2]] {{A "NullPointerException" could be thrown; "o2" is nullable here.}} flow@npe2 {{'o2' is dereferenced.}}
    }
  }

  private Object returnNullIfParamNotNull(Object o) {
    if (o != null) { // no flows
      return null;
    }
    return o;
  }

  void tst3(Object o) {
    if (o != null) {
      Object o2 = returnNullIfParamNotNull(o); // flow@npe3 {{'returnNullIfParamNotNull()' returns null.}} flow@npe3 {{Implies 'o2' is null.}}
      o2.toString(); // Noncompliant [[flows=npe3]] {{A "NullPointerException" could be thrown; "o2" is nullable here.}} flow@npe3 {{'o2' is dereferenced.}}
    }
  }
}

class MyException1 extends Exception {}
