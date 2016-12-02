class A {
  private void varArgMethod(C c, A... objects) {
    c.toString();
    objects.toString();
  }

  void callingVarArgMethod(A[] as, B[] bs, A a, B b, C c) {
    varArgMethod(c, as);
    varArgMethod(c, bs);

    varArgMethod(c, a);
    varArgMethod(c, a, b);

    varArgMethod(c);
    varArgMethod(c, null);
  }

  static class B extends A {}
  static class C {}
}
