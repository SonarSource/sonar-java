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

class VarArgs {

  public void qix(String a, Object... array) {
    qix(a, false, array); // should resolve to the other qix method, not as a recursive call.
  }

  private String qix(String a, boolean b, Object... array) {
    return a + b + array;
  }
}
