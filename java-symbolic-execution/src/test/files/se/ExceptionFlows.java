package org.foo;

abstract class A {

  boolean cond, cond2;

  private void methodA() throws ExA {
    if (cond) throw new ExA();
  }

  private void methodAB() throws ExA, ExB {
    if (cond) throw new ExA();
    else if (cond2) throw new ExB();
  }

  void test() {
    Object o = null; // flow@normal {{Implies 'o' is null.}}
    try {
      methodA();
    } catch (ExA e) {

    }
    o.toString(); // Noncompliant [[flows=normal]]  flow@normal {{'o' is dereferenced.}}
  }

  void test_multiple_ex_flows() {
    Object o = null; // flow@ex1,ex2 {{Implies 'o' is null.}}
    try {
      methodAB();  // flow@ex1 {{'ExA' is thrown.}} flow@ex2 {{'ExB' is thrown.}}
      o = new Object();
    } catch (ExA e) { // flow@ex1 {{'ExA' is caught.}}

    } catch (ExB e) { // flow@ex2 {{'ExB' is caught.}}

    }
    o.toString(); // Noncompliant [[flows=ex1,ex2]]  flow@ex1,ex2 {{'o' is dereferenced.}}
  }

  abstract void noBehavior() throws ExA, ExB;

  void test_method_with_no_behavior() {
    Object o = null; // flow@nb1,nb2 {{Implies 'o' is null.}}
    try {
      noBehavior();  // flow@nb1 {{'ExA' is thrown.}} flow@nb2 {{'ExB' is thrown.}}
      o = new Object();
    } catch (ExA e) { // flow@nb1 {{'ExA' is caught.}}

    } catch (ExB e) { // flow@nb2 {{'ExB' is caught.}}

    }
    o.toString(); // Noncompliant [[flows=nb1,nb2]]  flow@nb1,nb2 {{'o' is dereferenced.}}
  }

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

  class ExA extends Exception {}
  class ExB extends Exception {}
  class MyException extends Exception {}

  void test_finally(Object o, boolean b2) {
    try {
      o = null; // flow@finally {{Implies 'o' is null.}}
      call(); // flow@finally {{Exception is thrown.}}
      o = "";
    } finally {
      Object x = o;
      o.toString(); // Noncompliant [[flows=finally]] flow@finally {{'o' is dereferenced.}}
    }
  }

  void test_finally2(Object o, boolean b2) {
    try {
      o = null; // flow@finally2 {{Implies 'o' is null.}}
      call(); // flow@finally2 {{Exception is thrown.}}
      o = "";
    } finally {
      call();
      o.toString(); // Noncompliant [[flows=finally2]] flow@finally2 {{'o' is dereferenced.}}
    }
  }

  void test_finally3(Object o, boolean b2) {
    try {
      o = null; // flow@finally3 {{Implies 'o' is null.}}
      call(); // flow@finally3 {{Exception is thrown.}}
      o = "";
    } finally {
      try {
        call(); // flow@finally3 {{Exception is thrown.}}
        o = "";
      } finally {
        o.toString(); // Noncompliant [[flows=finally3]] flow@finally3 {{'o' is dereferenced.}}
      }
    }
  }

  private Object test_unknown_exception() throws ExA {
    try {
      return unknown_method(); // flow@unknown {{Exception is thrown.}}
    } catch (UnknownException ex) { // flow@unknown {{Exception is caught.}}
      return null;
    }
  }

  private void test_unknown() throws ExA {
    test_unknown_exception().toString(); // Noncompliant [[flows=unknown]] flow@unknown {{'test_unknown_exception()' can return null.}} flow@unknown {{Result of 'test_unknown_exception()' is dereferenced.}}
  }
}
