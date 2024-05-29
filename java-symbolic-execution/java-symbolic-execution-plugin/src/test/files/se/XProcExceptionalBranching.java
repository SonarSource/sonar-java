package org.foo.bar;

abstract class A {

  void tst(Object o) {
    try {
      foo(o);
    } catch (MyException1 e) {
      if (o == null) {}  // Noncompliant {{Remove this expression which always evaluates to "true"}}
    } catch (MyException2 e) {
      if (o == null) {}  // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    } finally {
      o.toString(); // Noncompliant  {{A "NullPointerException" could be thrown; "o" is nullable here.}}
    }
  }

  void tst2(Object o) {
    Object o1 = gul(o);
    o1.toString(); // Noncompliant  {{A "NullPointerException" could be thrown; "o1" is nullable here.}}
  }

  void tst3(Object o) {
    Object o1 = new Object();
    try {
      o1 = qix(o);
    } catch (MyException2 e) {
      if (o == null) {}  // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    }
    o1.toString(); // Compliant
  }

  private void foo(Object o) throws MyException1, MyException2 {
    if (o == null) {
      throw new MyException1();
    }
    bar();
  }

  abstract void bar() throws MyException2;

  private Object gul(Object o) {
    Object result = new Object();
    try {
      foo(o);
    } catch (MyException1 e) {
      result = o;
    } finally {
      return result;
    }
  }

  private Object qix(Object o) throws MyException2 {
    Object result;
    try {
      foo(o);
    } catch (MyException1 e) {
      result = o;
    } finally {
      result = new Object();
    }
    return result;
  }

}

class MyException1 extends Exception {}
class MyException2 extends Exception {}
