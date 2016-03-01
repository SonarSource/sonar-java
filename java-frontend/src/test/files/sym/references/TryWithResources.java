class A {

  void foo() {
    try (Stream foo3 = new Stream()) {
      foo3.bar();
    }
    try (Stream foo3 = new Stream()) {
      foo3.bar();
    }
  }
}