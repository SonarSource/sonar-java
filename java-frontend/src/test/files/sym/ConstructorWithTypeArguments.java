import java.io.Serializable;

class MyClass {
  <T extends I> MyClass(T t) {}
  <T extends J & I> MyClass(T t) {}

  void foo(B b, C c) {
    new<B>MyClass(b);
    new<C>MyClass(c);
  }
}

interface I {}
interface J {}

class B implements I {}
class C implements I, J {}
