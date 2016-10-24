class A {
  String foo(Object object) {
    try {
      return (String) object;
    } catch (ClassCastException cce) {
      return null;
    }
  }
}