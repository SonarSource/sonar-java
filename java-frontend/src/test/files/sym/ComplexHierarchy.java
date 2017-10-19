class A<V> extends B<V> implements I8<V> {

  @Override
  public I8<V> get() {
    return null;
  }
}

class B<U> implements I8<U>, I7<U>, I6<U>, I5<U>, I4<U>, I3<U> {
  @Override
  public I8<U> get() {
    return null;
  }
}
interface I<T> {
  I<T> get();
}
interface I2<S> extends I<I2<S>> {

}
interface I3<U> extends I<I2<I3<U>>> {}
interface I4<U> extends I3<U> {}
interface I5<U> extends I4<U>, I3<U> {}
interface I6<U> extends I5<U>, I4<U>, I3<U> {}
interface I7<U> extends I6<U>, I5<U>, I4<U>, I3<U> {}
interface I8<U> extends I7<U>, I6<U>, I5<U>, I4<U>, I3<U> {}

class Foo extends A<String> implements I8<String>{
  void test() {
    foo(get());
  }
  void foo(I<?> a) {

  }


}
