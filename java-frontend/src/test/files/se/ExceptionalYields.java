package org.foo;

abstract class A {
  private boolean myMethod(boolean b) throws MyException1, MyException2 {
    if (b) {
      throw new MyException1();
    }
    try {
      implicitException();
    } finally {
      // do nothing
    }
    return b;
  }

  abstract void implicitException() throws MyException2;
}

class MyException1 extends Exception {}
class MyException2 extends Exception {}
