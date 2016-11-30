class A {
  private void varArgMethod(A... objects) {
    objects.toString();
  }

  void callingVarArgMethod(A[] as, B[] bs, A a, B b) {
    varArgMethod(as);
    varArgMethod(bs);

    varArgMethod(a);
    varArgMethod(a, b);

    varArgMethod();
    varArgMethod(null);
  }

  static class B extends A {}
}
