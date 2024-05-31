class FP {
  void fun(Object a, Object b) {
    if (a == b) {
      if (b == null) {
        a.toString(); // Noncompliant
      }
    }
  }

  void fun_2ndlevel_transitive(Object a, Object b, Object c, Object d) {
    if (a == b) {
      if (c == d && b == c) {
        if (d == null) {
          a.toString(); // Noncompliant
        }
      }
    }
  }
}
