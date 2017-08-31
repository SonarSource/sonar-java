class A<K> {
  K id;
}

class B<J> extends A<J> { }

// two level of type hierarchy
class C extends B<Integer> {
  void fun() {
    foo(id);
  }
}

// one level of type hierarchy
class D extends A<Integer> {
  void fun() {
    foo(id);
  }
}
