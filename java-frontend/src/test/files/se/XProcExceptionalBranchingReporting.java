package org.foo.bar;

import javax.annotation.CheckForNull;

abstract class A {

  private void foo(boolean b, Object o) throws MyException1 {
    if (b // flow@catof2 {{Implies 'b' is true.}}
      && o == null) { // flow@npe,catof1 {{Implies 'o' is null.}}
      throw new MyException1();
    }
  }

  void tst(Object o, boolean b) {
    try {
      foo(b, o); // flow@npe,catof1 {{Implies 'o' is null.}}  flow@catof2 {{Implies 'b' is true.}}
    } catch (MyException1 e) {
      if (b) { // Noncompliant [[flows=catof2]] {{Change this condition so that it does not always evaluate to "true"}} flow@catof2  {{Condition is always true.}}
        if (o == null) {} // Noncompliant [[flows=catof1]] {{Change this condition so that it does not always evaluate to "true"}} flow@catof1 {{Condition is always true.}}
      }
    } finally {
      o.toString(); // Noncompliant [[flows=npe]] {{NullPointerException might be thrown as 'o' is nullable here}}  flow@npe {{'o' is dereferenced.}}
    }
  }

  @CheckForNull
  abstract void bar(Object o);

  void tst1(Object o) {
    Object o2 = bar(o); // flow@npe1 {{'bar()' returns null.}} flow@npe1 {{'o2' is assigned null.}}
    o2.toString(); // Noncompliant [[flows=npe1]] {{NullPointerException might be thrown as 'o2' is nullable here}} flow@npe1 {{'o2' is dereferenced.}}
  }

  private Object returnParam(Object o) {
    return o;
  }

  void tst2(Object o) {
    if (o == null) { // flow@npe2 {{Implies 'o' is null.}}
      Object o2 = returnParam(o); // flow@npe2 {{'o2' is assigned null.}}
      o2.toString(); // Noncompliant [[flows=npe2]] {{NullPointerException might be thrown as 'o2' is nullable here}} flow@npe2 {{'o2' is dereferenced.}}
    }
  }

  private Object returnNullIfParamNotNull(Object o) {
    if (o != null) { // no flows
      return null;
    }
    return o;
  }

  void tst3(Object o) {
    if (o != null) {
      Object o2 = returnNullIfParamNotNull(o); // flow@npe3 {{'returnNullIfParamNotNull()' returns null.}} flow@npe3 {{'o2' is assigned null.}}
      o2.toString(); // Noncompliant [[flows=npe3]] {{NullPointerException might be thrown as 'o2' is nullable here}} flow@npe3 {{'o2' is dereferenced.}}
    }
  }
}

class MyException1 extends Exception {}
