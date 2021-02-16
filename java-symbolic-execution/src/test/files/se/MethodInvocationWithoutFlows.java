class A {


  void test(Object o) {
    o = h(); // flow@t2 {{'h()' returns null.}} flow@t2 {{Implies 'o' is null.}}
    g(o); // Noncompliant
    o.toString(); // Noncompliant [[flows=t2]] flow@t2 {{'o' is dereferenced.}}
  }

  private void loop(Object o) {
    while (flag) {
      g(o); // Noncompliant
      o.toString();   // Noncompliant [[flows=loop]] flow@loop [[order=3]] {{'o' is dereferenced.}}
      o = h(); // flow@loop {{Implies 'o' is null.}} [[order=2]] flow@loop [[order=1]] {{'h()' returns null.}}
    }
  }

  private void g(Object o) {
    if (flag)
      o.toString();
  }

  private static Object h() {
    return null;
  }

  boolean flag;
}
