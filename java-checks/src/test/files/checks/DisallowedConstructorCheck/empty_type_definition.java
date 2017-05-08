class A {
  A(int i, long l, String[] s) {
  }
  A(){}

  void plop() {
    new A(1,2,new String[]{"1"});
    new A();
    new Object();
  }
}
