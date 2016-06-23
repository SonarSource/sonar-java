class A<T> {}
class B<U> extends A<U> {}
class Test {
  B<String> b;
  <S> S foo(A<S> a) {
    foo(b); // should be resolved with infered substitution S -> String
  }
}
