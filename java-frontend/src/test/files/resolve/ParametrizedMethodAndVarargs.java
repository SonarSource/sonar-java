class Parent<X> {}
class Child<Y> extends Parent<Y> {}

class A {}
class B extends A {}
class C extends A {}

class Test {
  <T> Parent<T> foo(Parent<? extends T> ... o) { return null; }
  <T> Parent<T> bar(Parent<T>... o) { return null; }

  Child<B> childB;
  Child<C> childC;

  void methodCall() {
    foo(childB, childC);
    bar(childB, childB);
  }
}
