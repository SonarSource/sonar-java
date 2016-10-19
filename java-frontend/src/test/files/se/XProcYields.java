class A {
  Object foo(Object o, boolean b) {
    if (b) {

      return o.toString();
    }
    o.toString();
    return null;
  }
}