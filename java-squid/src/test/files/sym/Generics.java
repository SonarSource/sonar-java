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
  }

  <P> P method3() {
    Object myObject = <String>method3();
  }

}