package checks;

import static java.lang.Boolean.TRUE;

class CallGetClassCheck_A {

  private static class B {}

  void foo() {
    CallGetClassCheck_A a1 = new CallGetClassCheck_A(); // Noncompliant {{Remove this object instantiation and use "CallGetClassCheck_A.class" instead.}}
    new CallGetClassCheck_B().getClass(); // Noncompliant {{Remove this object instantiation and use "CallGetClassCheck_B.class" instead.}}
    a1.getClass();
    new CallGetClassCheck_A().bar().getClass(); // Compliant
    Class clazz = CallGetClassCheck_A.class; // Compliant
    getClass(); // Compliant

    new CallGetClassCheck_C().getClass(); // Noncompliant {{Remove this object instantiation and use "CallGetClassCheck_C.class" instead.}}
    CallGetClassCheck_A a2 = new CallGetClassCheck_C(); // Noncompliant {{Remove this object instantiation and use "CallGetClassCheck_C.class" instead.}}
//                           ^^^^^^^^^^^^^^^^^^^^^^^^^
    a2.getClass();

    CallGetClassCheck_A a3 = new CallGetClassCheck_A(); // Compliant
    a3.foo();
    a3.getClass(); // Compliant

    CallGetClassCheck_D d = new CallGetClassCheck_D(); // Compliant
    d.getClass(null);

    new CallGetClassCheck_E() { // Noncompliant {{Remove this object instantiation and use "CallGetClassCheck_E.class" instead.}}
    }.getClass();
    CallGetClassCheck_E e = new CallGetClassCheck_E() { // Noncompliant {{Remove this object instantiation and use "CallGetClassCheck_E.class" instead.}}
    };
    e.getClass();

    new CallGetClassCheck_I() { // Noncompliant {{Remove this object instantiation and use "CallGetClassCheck_I.class" instead.}}
      @Override
      public void foo() {
      }
    }.getClass();
    CallGetClassCheck_I i = new CallGetClassCheck_I() { // Noncompliant {{Remove this object instantiation and use "CallGetClassCheck_I.class" instead.}}
      @Override
      public void foo() {
      }
    };
    i.getClass();

    new CallGetClassCheck_F().getClass(); // Noncompliant {{Remove this object instantiation and use "CallGetClassCheck_F.class" instead.}}
    CallGetClassCheck_F<CallGetClassCheck_A> f1 = new CallGetClassCheck_F<CallGetClassCheck_A>(); // Noncompliant {{Remove this object instantiation and use "CallGetClassCheck_F.class" instead.}}
    f1.getClass();

    this.getClass(); // Compliant

    B b = new CallGetClassCheck_A().bar();
    b.getClass(); // Compliant

    Class arrayObject = new Object[0].getClass(); // Noncompliant {{Remove this object instantiation and use "Object[].class" instead.}}
    Class arrayLong = new Long[0].getClass(); // Noncompliant {{Remove this object instantiation and use "Long[].class" instead.}}
    Class arrayObject2 = Object[].class; // Compliant
  }

  B bar() {
    return new B();
  }

  void pom(CallGetClassCheck_A a) {
    a.getClass(); // Compliant
  }
}

class CallGetClassCheck_B {
}

class CallGetClassCheck_C extends CallGetClassCheck_A {

  void foo() {
    super.getClass(); // Compliant
  }
}

class CallGetClassCheck_D {
  String getClass(Object obj) {
    return "";
  }
}

abstract class CallGetClassCheck_E {
}

class CallGetClassCheck_F<T> {
  T foo() {
    TRUE.getClass();
    return null;
  }
}

interface CallGetClassCheck_I {
  void foo();
}
