package org.foo;

class A {
  void foo(boolean a, Object b) throws MyException {
    Object o = bar(a, b);
    if (a) {
      o.toString();
    }
  }

  private Object bar(boolean b, Object o) throws MyException {
    if (b) {
      return null;
    }
    if (o == null)  {
      throw new MyException();
    }
    return o;
  }
}

class MyException extends Exception { }
