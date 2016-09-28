class A {}

class B {
  void test() {
    new Thread(this::bar1);
    new Thread(this::bar1, "name");
  }

  private void bar1() {}
}

class C {
  private void build(java.util.List<? extends A> as) {
    as.forEach(this::bar);
  }

  private void bar(A a) {}
}
