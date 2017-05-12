class A {
  A field;

  void foo(Object arg0, Object arg1, Object arg2) {
    A localVariable = new A();
    foo(localVariable, this.field, new A());
  }
}
