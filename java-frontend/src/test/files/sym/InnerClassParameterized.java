class A<T> {
  public void foo(MyClass<T> mc) {
    new InnerA<>((A<T>) this, mc); // 'this' is explicitly casted to the parameterized type, which should be equivalent
  }

  private static class InnerA<U> {
    private InnerA(A<U> a, MyClass<U> mc) { }
  }
}

class B<T> {
  public void foo(MyClass<T> mc) {
    new InnerB<T>(this, mc); // type argument 'T' is written explicitly
  }

  private static class InnerB<U> {
    private InnerB(B<U> b, MyClass<U> mc) { }
  }
}

class C<T> {
  public void foo(MyClass<T> mc) {
    new InnerC<>(this, mc); // uses diamond operator instead of argument 'T'
  }

  private static class InnerC<U> {
    private InnerC(C<U> c, MyClass<U> mc) { }
  }
}

class D<T> {
  public void foo(MyClass<T> mc) {
    new InnerD<>((D) this, mc); // 'this' is explicitly casted to the raw type
  }

  private static class InnerD<U> {
    private InnerD(D<U> a, MyClass<U> mc) { }
  }
}

class MyClass<W> {}
