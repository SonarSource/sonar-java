class A {
  void plop() {
    boolean a = true;
    a &= (b1() == C);
    a &= (b2() == C);
    a &= (b3() == C);
    a &= (b4() == C);
    a &= (b5() == C);
    a &= (b6() == C);
    a &= (b7() == C);
    a &= (b8() == C);
    a &= (b9() == C);
    a &= (b10() == C);
    a &= (b11() == C);
    a &= (b12() == C);
    a &= (b13() == C);
    a &= (b14() == C);
    a &= (b15() == C);
    a &= (b16() == C);
    a &= (b17() == C);
    a &= (b18() == C);
    a &= (b19() == C);
    a &= (b20() == C);
    a &= (b21() == C);
    a &= (b22() == C);
    a &= (b23() == C);
    a &= (b24() == C);
    a &= (b25() == C);
    a &= (b26() == C);
    a &= (b27() == C);
    a &= (b28() == C);

    if (a) { //BOOM : 2^n -1 states are generated (where n is the number of lines of &= assignements in the above code) -> fail fast by not even enqueuing nodes
    }
  }
}
