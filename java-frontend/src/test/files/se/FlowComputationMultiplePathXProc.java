abstract class A {

  private void f(boolean arg) {
    if (cond) {
      if (arg) return; // flow@xproc1 {{Implies 'arg' is true.}}
    } else {
      if (arg) return; // flow@xproc2 {{Implies 'arg' is true.}}
    }
    throw new RuntimeException();
  }

  void test(boolean a) {
    f(a);  // flow@xproc1,xproc2 {{Implies 'a' is true.}}
    if (a) { // Noncompliant [[flows=xproc1,xproc2]] flow@xproc1,xproc2 {{Condition is always true.}}

    }
  }

}
