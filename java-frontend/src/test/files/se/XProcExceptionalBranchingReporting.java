package org.foo.bar;

import javax.annotation.CheckForNull;

abstract class A {

  private void foo(Object o) throws MyException1 {
    if (o == null) { // flow@catof {{...}} flow@npe {{...}}
      throw new MyException1();
    }
  }

  void tst(Object o) {
    try {
      foo(o); // flow@catof {{Exception 'MyException1' thrown from method invocation [see L#7].}} flow@npe {{Exception 'MyException1' thrown from method invocation [see L#7].}}
    } catch (MyException1 e) {
      if (o == null) {}  // Noncompliant [[flows=catof]] {{Change this condition so that it does not always evaluate to "true"}} flow@catof {{Condition is always true}} flow@npe {{...}}
    } finally {
      o.toString(); // Noncompliant [[flows=npe]] {{NullPointerException might be thrown as 'o' is nullable here}} flow@npe {{o is dereferenced}}
    }
  }

  void tst2(Object o) {
    Object o2 = // flow@npe1 {{o2 is assigned null}}
      bar(o); // flow@npe1 {{...}}
    o2.toString(); // Noncompliant [[flows=npe1]] {{NullPointerException might be thrown as 'o2' is nullable here}} flow@npe1 {{o2 is dereferenced}}
  }

  @CheckForNull
  abstract void bar(Object o);

  private Object returnParam(Object o) {
    return o;
  }

  void tst2(Object o) {
    if (o == null) { // flow@npe2 {{...}}
      Object o2 = returnParam(o); // flow@npe2 {{o2 is assigned null}}
      o2.toString(); // Noncompliant [[flows=npe2]] {{NullPointerException might be thrown as 'o2' is nullable here}} flow@npe2 {{o2 is dereferenced}}
    }
  }

  void tst3(Object o) {
    if (o != null) {
      Object o2 = // flow@npe3 {{o2 is assigned null}}
        returnNullIfParamNotNull(o); // flow@npe3 {{Uses return value [see L#51].}}
      o2.toString(); // Noncompliant [[flows=npe3]] {{NullPointerException might be thrown as 'o2' is nullable here}} flow@npe3 {{o2 is dereferenced}}
    }
  }

  private Object returnNullIfParamNotNull(Object o) {
    if (o != null) { // flow@npe3 {{...}}
      return null;
    }
    return o;
  }

  void test4(Object o, boolean b) {
    if (b) {
      Object o2 = returnParamIfTrueAndNotNull(b, o); // flow@npe4 {{Learns from method call [see L#65].}}
      o.toString(); // Noncompliant [[flows=npe4]] {{NullPointerException might be thrown as 'o' is nullable here}} flow@npe4 {{o is dereferenced}}
    }
  }

  private Object returnParamIfTrueAndNotNull(boolean b, Object o) {
    if (!b) {
      return new Object();
    }
    if (o != null) { // flow@npe4 {{...}}
      return o;
    }
    return new Object();
  }

}

class MyException1 extends Exception {}
