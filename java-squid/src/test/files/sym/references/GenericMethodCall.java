interface A extends B {}
interface B extends C<String> {}
interface C<T> extends D<T> {}
interface D<E> {
  E get();
}


class X extends Y{}
class Y extends W<String> {}
class W<T> extends Z<T> {}
class Z<E> {
  E get() {
    return null;
  }
}


class Test {
  A a;
  X x;
  void fun(String s) {
    fun(a.get());
    fun(x.get());
  }
}
