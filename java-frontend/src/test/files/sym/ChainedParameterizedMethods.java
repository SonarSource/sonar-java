class A {
  public void newBounds(Tuple<Object, A> rests) {
    newBounds(foo(new Object(), bar()));
  }

  <T, U> Tuple<T, U> foo(T t, U u) { return null; }
  <V extends A> V bar() { return null; }
}

class Tuple<U, V> { }
