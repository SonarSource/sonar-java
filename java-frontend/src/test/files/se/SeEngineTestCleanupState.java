class A0 {

  boolean boolMethod() {
    return new java.util.Random().nextBoolean();
  }

  void test_reduced_steps(Object c) {
    Object a = new Object();
    Object b;
    if (boolMethod()) {
      b = new Object();
    }
    a.toString();
  }
}
