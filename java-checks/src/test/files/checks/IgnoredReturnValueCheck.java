class A {
  void voidMethod() {}
  int intMethod() {}
  UnknownType unknownTypeMethod() {}
  void foo() {
    int a = intMethod(); //Compliant
    intMethod(); //NonCompliant
    voidMethod(); //Compliant
    new A().intMethod();//NonCompliant
    new A().voidMethod();//Compliant
    unknownTypeMethod();//Compliant type is unknown
    unresolvedMethod();//Compliant method is not resolved so type is unknown
  }


}