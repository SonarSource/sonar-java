package org.foo;

abstract class A {
  private void myVoidMethod(Object o) throws MyException1, MyException2 {
    if (o == null) {
      throw new MyException1();
    }
    try {
      implicitException();
    } finally {
      // do nothing
    }
  }

  abstract void implicitException() throws MyException2;
}

class MyException1 extends Exception {}
class MyException2 extends Exception {}
