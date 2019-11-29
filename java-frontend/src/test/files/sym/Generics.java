class A<T, S extends CharSequence> {
  T field1;
  S field2;

  A<String, String> a1 = new A<String, String>();
  A<String, String> a2 = new A<String, String>();
  A<Integer, String> a3 = new A<Integer, String>();
  void foo(String s) {}
  void foo(Integer i){}

  void bar() {
    foo(a1.field1);
    foo(a3.field1);
  }

  S[] arrayErasure;

  class B<U> extends A<C<String>, String> {
    void fun() {
      field1 = null;
    }
  }

  class C<V> {
    S innerClassField;
  }



  S method1(S param) {
    return param;
  }
  <P> P method2(P plop) {
    return plop;
  }

  <P> P method3() {
    Object myObject = this.<String>method3();
    return null;
  }

  <Q> C<Q> method4() {
    Object myObject = this.<String>method4();
    return null;
  }

  class D<V> {
    V field;
  }

  D<D<T>> ddt;
  void ddt_method() {
    Object obj = a1.ddt.field.field;
  }

  class E {
    void method_of_e(){}
  }

  void e_method() {
    A.<E>methodStatic().method_of_e();
  }
  static <T> T methodStatic() { return null; };
  void unknownSymbol() {
    Foo<String> foo;
  }

  private static class Foo<T> { }
}


class MyClass implements MyInterface<MyClass.B<Object>> {
  public static class B<T> extends C<T> {
  }
}

interface MyInterface<T> {
}

class C<T> {
}

class TypeParameterUsedInMethods<T, U> {
  Function<? super T, ? extends U> getter;

  void foo(T val, U wantedValue) {
    getter.apply(val);
    // these 2 calls do not compile but methods are resolved, as argument matching is based on erasure
    getter.apply(new Object());
    getter.apply(wantedValue);
    // not valid call with different erasure, not resolved
    getter.apply("hello");
    new TypeParameterUsedInMethods<Object, Object>().getter.apply(new Object());
  }

  static class Function<X, Y> {
    Y apply(X from) {
      return null;
    }
  }
}

class ExtendedTypeParam {
  class MyClass {}
  interface I {}
  interface J {}
  class ParametrizedClass<W, X extends MyClass, Y extends I, Z extends MyClass & I & J> {}
}
