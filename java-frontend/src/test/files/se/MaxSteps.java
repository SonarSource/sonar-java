class A {
  void plop() {
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
    }
  }
}
