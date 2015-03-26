import static java.lang.Boolean.*;
class A { void foo() {
    A a1 = new A(); // Noncompliant
    a1.getClass();
    new B().getClass(); // Noncompliant
    new A().bar().getClass(); // Compliant
    Class clazz = A.class; // Compliant
    getClass(); // Compliant

    new C().getClass(); // Noncompliant
    A a2 = new C(); // Noncompliant
    a2.getClass();

    A a3 = new A(); // Compliant
    a3.foo();
    a3.getClass(); // Compliant

    D d = new D(); // Compliant
    d.getClass(null);

    new E() { // Noncompliant
    }.getClass();
    E e = new E() { // Noncompliant
    };
    e.getClass();

    new I() { // Noncompliant
      @Override
      public void foo() {
      }
    }.getClass();
    I i = new I() { // Noncompliant
      @Override
      public void foo() {
      }
    };
    i.getClass();

    new F().getClass(); // Noncompliant
    F<A> f1 = new F<A>();
    f1.getClass(); // Noncompliant

    this.getClass(); // Compliant

    B b = new A().bar();
    b.getClass(); // Compliant
  }

  B bar() {
    return new B();
  }

  void pom(A a) {
    a.getClass(); // Compliant
  }
}

class B {
}

class C extends A {

  void foo() {
    super.getClass(); // Compliant
  }
}

class D {
  String getClass(Object obj) {
    return "";
  }
}

abstract class E {
}

class F<T> {
  T foo() {
    TRUE.getClass();
    return null;
  }
}

interface I {
  void foo();
}