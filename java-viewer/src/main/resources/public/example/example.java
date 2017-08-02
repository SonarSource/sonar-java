class A {
  void foo(boolean a) {
    Object o = bar(a);
    if (a) {
      o.toString();
    }
  }

  private Object bar(boolean b) {
    if (b) {
      return null;
    }
    return new Object();
  }
}
