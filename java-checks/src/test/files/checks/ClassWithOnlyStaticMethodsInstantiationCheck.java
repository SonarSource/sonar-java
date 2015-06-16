import java.nio.channels.IllegalSelectorException;

class Tester {
  void bar() {
    A a = new A(); // Noncompliant {{Remove this instantiation of "A".}}
    D d = new D(); // Noncompliant {{Remove this instantiation of "D".}}
    E e = new E(); // Noncompliant {{Remove this instantiation of "E".}}
    F f = new F(); // Noncompliant {{Remove this instantiation of "F".}}
    InnerClass i1 = new InnerClass(); // Noncompliant {{Remove this instantiation of "InnerClass".}}
    Tester.InnerClass i2 = new Tester.InnerClass(); // Noncompliant {{Remove this instantiation of "InnerClass".}}
    J<A> j = new J<A>(); // Noncompliant {{Remove this instantiation of "J".}}
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

  void foo() throws NumberFormatException {
    throw new NumberFormatException(); // Compliant
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

  public static void foo() {
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

class M {
  M() {
  }
  public static M foo() {
    return new M(); // Compliant
  }
}
