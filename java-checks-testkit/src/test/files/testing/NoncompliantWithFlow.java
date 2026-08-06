class A {
  void method() {
    Object a = null; // flow@f1
    a.toString(); // Noncompliant [[flows=f1]]
  }
}
