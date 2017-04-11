class A<T extends Throwable> {
  void foo() throws T {

  }
  void test() {
    new A<java.io.IOException>().foo();
  }
}

