package org.foo;

abstract class A {
  Object foo(boolean a) {
    Object o = null;
    try {
      o = bar(a);
    } catch (MyException0 e) {
      if (a) {
        return new Object();
      }
    } catch (Exception e) {
      return null;
    }
    return o;
  }

  private Object bar(boolean b) throws MyException0, MyException1, MyException2 {
    if (b) {
      throw new MyException0();
    }
    if (qix()) {
      throw new MyException1();
    }
    if (gul()) {
      throw new MyException2();
    }
    return null;
  }

  abstract boolean qix();
  abstract boolean gul();

}
class MyException0 extends Exception {}
class MyException1 extends Exception {}
class MyException2 extends Exception {}

