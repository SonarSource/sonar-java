class TestClass {
  <T> Child<A<T>> foo(Parent<? extends B<T>> b) {
    return null;
  }

  void callingMethod(Parent<B<String>> b, Child<C<Integer>> d) {
    usesString(foo(b));
    usesInteger(foo(d));
  }

  void usesString(Child<A<String>> a) {}
  void usesInteger(Child<A<Integer>> a) {}
}

interface A<X> {}
interface B<Y> {}
interface Parent<Z> {}
interface Child<E> extends Parent<E> {}
class C<W> implements B<W> {}
