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

  private boolean multipleArgs(boolean x, boolean y) {
    if (x && y) return true;  // flow@args {{Implies 'x' is true.}} flow@args {{Implies 'y' is true.}}
    return false;
  }

  void test_constraint_on_multiple_args(boolean a, boolean b) {
    if (multipleArgs(a, a)) { // flow@args {{Implies 'a' is true.}} flow@args {{Implies 'a' is true.}}
      if (a) { // Noncompliant [[flows=args]] flow@args {{Condition is always true.}}

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
