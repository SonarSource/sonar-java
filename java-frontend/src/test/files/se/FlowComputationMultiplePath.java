abstract class A {

  void f() {
    Object a;
    Object b = null; // flow@path2 {{'b' is assigned null.}}
    if (cond) {
      a = null; // flow@path1 {{'a' is assigned null.}}
    } else {
      a = b; // flow@path2 {{'a' is assigned null.}}
    }
    a.toString(); // Noncompliant [[flows=path1,path2]] flow@path1,path2 {{'a' is dereferenced.}}
  }

  void g() {
    Object a;
    Object b = null; // flow@p1 {{'b' is assigned null.}}
    if (cond) {
      a = null; // flow@p2 {{'a' is assigned null.}}
    } else {
      a = b; // flow@p1 {{'a' is assigned null.}}
    }
    Object c;
    if (cond) {
      c = null;  // flow@p3 {{'c' is assigned null.}}
    } else {
      c = a; // flow@p1,p2 {{'c' is assigned null.}}
    }
    c.toString(); // Noncompliant [[flows=p1,p2,p3]] flow@p1,p2,p3 {{'c' is dereferenced.}}
  }

  private boolean multipleArgs(boolean x, boolean y) { // flow@args [[order=3]] {{Implies 'x' has the same value as 'a'.}} flow@args [[order=4]] {{Implies 'y' has the same value as 'a'.}}
    if (x && y) return true;  // flow@args [[order=5]] {{Implies 'x' is true.}} flow@args [[order=6]] {{Implies 'y' is true.}}
    return false;
  }

  void test_constraint_on_multiple_args(boolean a, boolean b) {
    if (multipleArgs(a, a)) { // flow@args [[order=1]] {{'a' is passed to 'multipleArgs()'.}} flow@args [[order=2]] {{'a' is passed to 'multipleArgs()'.}} flow@args [[order=7]] {{Implies 'a' is true.}} flow@args [[order=8]] {{Implies 'a' is true.}}
      if (a) { // Noncompliant [[flows=args]] flow@args [[order=9]] {{Expression is always true.}}

      }
    }
  }

  void loop() {
    Object a;
    while (true) {
      a = null; // flow@loop {{'a' is assigned null.}}
      if (cond) a.toString(); // Noncompliant [[flows=loop]] flow@loop {{'a' is dereferenced.}}
    }
  }

}
