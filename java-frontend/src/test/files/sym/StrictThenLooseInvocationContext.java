class A {
  void foo(int i) { }
}

class B extends A { }

class C extends B {
  void bar() {
    foo(1); // should resolve to A.foo(int), by strict invocation, rather than C.foo(Integer), which would be resolved by loose invocation
  }

  void foo(Integer i) { }
}
