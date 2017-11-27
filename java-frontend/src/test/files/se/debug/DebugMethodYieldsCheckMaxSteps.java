class A {

  static Object foo1(boolean b) { // Noncompliant - complete in less than 20 steps
    if (b) {
      return null;
    }
    return new Object();
  }

  static Object plop() { // Compliant - too many steps
    boolean a = true;
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
