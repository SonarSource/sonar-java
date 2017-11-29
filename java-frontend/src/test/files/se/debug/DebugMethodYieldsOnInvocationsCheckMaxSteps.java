class A {
  static boolean foo(boolean a) {
    if (a) {
      return false;
    }
    return true;
  }

  // method does not complete but yields are still reported
  void bar(boolean a) {
    boolean a = foo(a); // Noncompliant {{Method 'foo' has 2 method yields.}}
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);

    if (a) { //BOOM : 2^n -1 states are generated (where n is the number of lines of &= assignements in the above code) -> fail fast by not even enqueuing nodes
      return new Object();
    }
    return null;
  }
}
