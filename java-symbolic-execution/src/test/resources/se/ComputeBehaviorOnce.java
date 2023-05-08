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
    a &= (Math.random() == 1.0d);
    a &= (Math.random() == 2.0d);
    a &= (Math.random() == 3.0d);
    a &= (Math.random() == 4.0d);
    a &= (Math.random() == 5.0d);
    a &= (Math.random() == 6.0d);
    a &= (Math.random() == 7.0d);
    a &= (Math.random() == 8.0d);
    a &= (Math.random() == 9.0d);
    a &= (Math.random() == 10.0d);
    a &= (Math.random() == 11.0d);
    a &= (Math.random() == 12.0d);
    a &= (Math.random() == 13.0d);
    a &= (Math.random() == 14.0d);

    if (a) { //BOOM : 2^n -1 states are generated (where n is the number of lines of &= assignements in the above code) -> fail fast by not even enqueuing nodes
    }
  }


}
