import static java.lang.Boolean.*;
class A { void foo() {
    A a1 = new A(); // Noncompliant {{Remove this object instantiation and use "A.class" instead.}}
    new B().getClass(); // Noncompliant {{Remove this object instantiation and use "B.class" instead.}}
    a1.getClass();
    new A().bar().getClass(); // Compliant
    Class clazz = A.class; // Compliant
    getClass(); // Compliant

    new C().getClass(); // Noncompliant {{Remove this object instantiation and use "C.class" instead.}}
    A a2 = new C(); // Noncompliant [[sc=12;ec=19]] {{Remove this object instantiation and use "C.class" instead.}}
    a2.getClass();

    A a3 = new A(); // Compliant
    a3.foo();
    a3.getClass(); // Compliant

    D d = new D(); // Compliant
    d.getClass(null);

    new E() { // Noncompliant {{Remove this object instantiation and use "E.class" instead.}}
    }.getClass();
    E e = new E() { // Noncompliant {{Remove this object instantiation and use "E.class" instead.}}
    };
    e.getClass();

    new I() { // Noncompliant {{Remove this object instantiation and use "I.class" instead.}}
      @Override
      public void foo() {
      }
    }.getClass();
    I i = new I() { // Noncompliant {{Remove this object instantiation and use "I.class" instead.}}
      @Override
      public void foo() {
      }
    };
    i.getClass();

    new F().getClass(); // Noncompliant {{Remove this object instantiation and use "F.class" instead.}}
    F<A> f1 = new F<A>(); // Noncompliant {{Remove this object instantiation and use "F.class" instead.}}
    f1.getClass(); 

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
