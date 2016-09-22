package org.foo;

class B {
  void test() {
    new Thread(this::bar1);
    new Thread(this::bar1, "name");
  }

  private void bar1() {}
}
