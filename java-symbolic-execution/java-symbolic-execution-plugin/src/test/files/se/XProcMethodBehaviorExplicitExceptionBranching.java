package org.foo.bar;

class A {

  private void foo(Object o1, Object o2) throws MyException1, MyException2, MyException3 {
    if (o1 == null) {
      throw new MyException1();
    }
    if (o2 == null) {
      throw new MyException2();
    }
    if (o2.equals(o1)) {
      throw new MyException3();
    }
    // do something
  }

  void tst(Object o1, Object o2) {
    try {
      foo(o1, o2);
    } catch (MyException1 e) {
      if (o1 == null) {} // Noncompliant {{Remove this expression which always evaluates to "true"}}
    } catch (MyException3 e) {
      if (o1 == null  // Noncompliant {{Remove this expression which always evaluates to "false"}}
        || o2 == null) {}
    } catch (MyException2 e) {
      if (o2 != null) {}
      o1.toString(); // Compliant - can not be null
    }
  }
}

class MyException1 extends Exception {}
class MyException2 extends Exception {}
class MyException3 extends MyException2 {}
