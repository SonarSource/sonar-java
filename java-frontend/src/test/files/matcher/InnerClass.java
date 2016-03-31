package org.sonar.test;
class Outer {

  class Inner {
    void foo() {}
  }


  void test() {
    new Inner().foo();
  }
}