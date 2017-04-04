class ComputeBehaviorOnce {

  private static int foo(int a) {
    if(a == 0) {
      return a;
    }
    return bar(a - 1);
  }


  private static int bar(int a) {
    return qix(a);
  }

  private static int qix(int a) {
    return foo(a);
  }

  private static void multipleCall() {
    plop();
    plop();
  }

  private void plop() {
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
