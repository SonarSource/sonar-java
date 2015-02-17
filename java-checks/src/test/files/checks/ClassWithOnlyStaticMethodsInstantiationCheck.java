class Tester {
  void bar() {
    A a = new A(); // Noncompliant
    D d = new D(); // Noncompliant
    E e = new E(); // Noncompliant
    F f = new F(); // Noncompliant
    H h = new H(); // Noncompliant
    InnerClass i1 = new InnerClass(); // Noncompliant
    Tester.InnerClass i2 = new Tester.InnerClass(); // Noncompliant
    J<A> j = new J<A>(); // Noncompliant
    B b = new B(); // Compliant
    C c = new C(); // Compliant
    G g = new G(); // Compliant
    K k = new K(); // Compliant
    L l = new L(); // Compliant
  }

  static class InnerClass {
    static void foo() {
    }
  }
}

class A {
  A() {
  }

  static void foo() {
  }
}

class B {
}

class C extends A {
}

class D extends A {
  static int val;

  static void bar() {
  }
}

class E {
  static int val;

  static void foo() {
  }
}

class F extends A {
  static void bar() {
  }
}

class G {
  int val;

  void foo() {
  }
}

class H extends G {
  static void bar() {
  }
}

class J<T> {
  static void foo() {
  }
}

class K {
  enum myEnum {
    FOO
  }
}

class L {
  class M {
  }
}
