package org.foo;

abstract class A {

  void foo(Object o1, Object o2) throws Exception {
    try {
      m1(o1, o2);
    } catch (Ex1 e) {
      m2();
      m3();
    } catch (Ex2 e) {
      if (o2 == null) {
        m4();
      }
    } catch (Ex3 e) {
      String res = m5();
    } finally {
      m6();
    }
  }

  abstract void m1(Object o1, Object o2) throws Ex1, Ex2, Ex3;
  abstract void m2() throws Exception;
  abstract void m3();
  abstract void m4();
  abstract String m5() throws Exception;
  abstract void m6();
}

class Ex1 extends Exception {}
class Ex2 extends Exception {}
class Ex3 extends Exception {}
