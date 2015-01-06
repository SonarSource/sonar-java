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
}