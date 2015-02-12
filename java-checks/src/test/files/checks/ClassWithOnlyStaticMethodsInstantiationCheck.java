class A {
  static int foo() {
    return 0;
  }
}

class B {
}

class C extends A {
}

class D extends A {
  static int val = 0;

  static int bar() {
    return 0;
  }
}

class E {
  static int val = 0;

  static int foo() {
    return 0;
  }
}

class F extends A {
  static int bar() {
    return 0;
  }
}

class G {
  int val;

  void foo() {
  }
}

class H extends G {
  static int bar() {
    return 0;
  }
}

class Tester {
  void bar() {
    A a = new A(); // Noncompliant
    F f = new F(); // Noncompliant
    H h = new H(); // Noncompliant
    B b = new B(); // Compliant
    C c = new C(); // Compliant
    D d = new D(); // Compliant
    E e = new E(); // Compliant
    G g = new G(); // Compliant
  }
}