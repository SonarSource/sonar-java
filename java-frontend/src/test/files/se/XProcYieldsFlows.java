abstract class A {
  static Object foo(boolean b, Object o) {
    if (b) {
      Object o1 = o;
      if (tst(o1, b)) {
        o1 = new Object();
      }
      return o1;
    }
    o.toString();
    return null;
  }

  abstract boolean tst(Object o, boolean b);
}
