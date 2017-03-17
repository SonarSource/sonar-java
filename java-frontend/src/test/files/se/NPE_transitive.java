class FP {
  void fun(Object a, Object b) {
    if (a == b) {
      if (b == null) {
        a.toString(); // Noncompliant
      }
    }
  }
}
