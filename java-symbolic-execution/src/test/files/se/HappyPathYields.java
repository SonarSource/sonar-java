class A {
  static Object foo(Object o, boolean b) {
    if (b) {

      return o.toString();
    }
    o.toString();
    return null;
  }

  static void bar(boolean b, Object o) {
    if (b) {
      o.toString();
    }
  }

}
