class A {
  void test(String s) {
    ClassLoader cl = s.getClass().getClassLoader();
    cl.toString();
    Class.forName("blabal");
    wait();
  }
}
