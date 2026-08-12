class A {
  void method() {
    Object a = null; // flow@f1 [[ec=99]] {{null}}
    a.toString(); // Noncompliant [[flows=f1]]
  }
}
