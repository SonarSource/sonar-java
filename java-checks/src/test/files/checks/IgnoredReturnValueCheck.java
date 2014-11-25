class A {
  void voidMethod() {}
  int intMethod() {}

  void foo() {
    int a = intMethod(); //Compliant
    intMethod(); //NonCompliant
    voidMethod(); //Compliant

  }


}