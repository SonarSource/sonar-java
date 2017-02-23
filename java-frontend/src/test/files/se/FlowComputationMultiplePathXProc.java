abstract class A {

  // yield [arg -> true] has two flows
  private void one_yield_two_flows(boolean arg) {
    if (cond) {
      if (arg) return; // flow@xproc1 {{Implies 'arg' is true.}}
    } else {
      if (arg) return; // flow@xproc2 {{Implies 'arg' is true.}}
    }
    throw new RuntimeException();
  }

  void test(boolean a) {
    one_yield_two_flows(a);  // flow@xproc1,xproc2 {{Implies 'a' is true.}}
    if (a) { // Noncompliant [[flows=xproc1,xproc2]] flow@xproc1,xproc2 {{Condition is always true.}}

    }
  }

  // two exceptional yields [arg -> true, cond -> true] [arg -> true, cond -> false]
  // two normal yields [arg -> false, cond -> true] [arg -> false, cond -> false]
  private void two_yields_one_flow_each(boolean arg, boolean cond) throws MyEx {
    if (cond) {
      if (arg) throw new MyEx(); // flow@ex1 {{Implies 'arg' is true.}} flow@normal1 {{Implies 'arg' is false.}}
    } else {
      if (arg) throw new MyEx(); // flow@ex2 {{Implies 'arg' is true.}} flow@normal2 {{Implies 'arg' is false.}}
    }
    return;
  }

  void test_two_yields(boolean a, boolean cond) {
    try {
      two_yields_one_flow_each(a, cond); // flow@ex1,ex2 {{Implies 'a' is true.}} flow@normal1,normal2 {{Implies 'a' is false.}}
    } catch (MyEx e) {
      if (a) { // Noncompliant [[flows=ex1,ex2]] flow@ex1,ex2 {{Condition is always true.}}
        return;
      }
    }
    if (a) { // Noncompliant [[flows=normal1,normal2]] flow@normal1,normal2 {{Condition is always false.}}

    }
  }

  class MyEx extends Exception {
  }


}
