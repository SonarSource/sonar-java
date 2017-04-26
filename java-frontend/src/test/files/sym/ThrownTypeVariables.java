class A<T extends Throwable> {
  void foo() throws T {

  }
  void test() {
    new A<java.io.IOException>().foo();
  }
}

class B<T> {
  public <X extends Throwable> T bar(MyInterface<? extends X> ex) throws X {
    return null;
  }
}

@FunctionalInterface
interface MyInterface<T> {
  T get();
}

class Test {
  void test(MyInterface<java.util.NoSuchElementException> s) {
    new B<String>().bar(s);
    new B<String>().bar(java.io.IOException::new);
  }
}
