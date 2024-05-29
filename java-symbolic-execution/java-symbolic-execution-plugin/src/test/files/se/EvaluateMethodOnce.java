class A {

  void simple_condition(Object a) {
    new A() {
      void foo() {
        a.toString();
      }
    };
  }


}