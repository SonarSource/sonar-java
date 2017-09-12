package org.foo;

abstract class A {

  void foo() {
    A a = null; // flow@single_flow
    try {
      doSomething(); // equivalent flow using exception thrown here discarded
    } catch (MyException e) {
      log(e.getMessage());
    }
    a.call(); // Noncompliant [[flows=single_flow]] flow@single_flow
  }

  abstract void doSomething() throws MyException;

  abstract void log(String s);
  abstract void call();
}

class MyException extends Exception {
}
