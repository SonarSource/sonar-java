package org.foo.bar;

abstract class A {

  void tst(Object o) {
    try {
      foo(o);
    } catch (MyException1 e) {
      if (o == null) {}  // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
    } catch (MyException2 e) {
      if (o == null) {}  // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    } finally {
      o.toString(); // Noncompliant  {{NullPointerException might be thrown as 'o' is nullable here}}
    }
  }

  private void foo(Object o) throws MyException1, MyException2 {
    if (o == null) {
      throw new MyException1();
    }
    bar();
  }

  abstract void bar() throws MyException2;
}

class MyException1 extends Exception {}
class MyException2 extends Exception {}
