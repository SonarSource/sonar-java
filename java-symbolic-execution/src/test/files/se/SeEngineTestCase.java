class A {
  Object b;
  void foo() {
    e
        .f
        .g
        .h();
    for(int j = 0;j<10;j++) {
      int i = 1 + 2 + 3;
      Object a = b = new Object();
      printState();
      System.out.print(a);
    }
  }

  private static void printState() {
  }
}
