class A {
  void voidMethod() {}
  int intMethod() {}
  UnknownType unknownTypeMethod() {}
  void foo() {
    int a = intMethod(); //Compliant
    intMethod(); //Compliant
    voidMethod(); //Compliant
    new A().intMethod();//Compliant
    new A().voidMethod();//Compliant
    unknownTypeMethod();//Compliant type is unknown
    unresolvedMethod();//Compliant method is not resolved so type is unknown
    fluentMethod(""); //Compliant
    Integer.valueOf("1").byteValue(); //NonCompliant
    "plop".replace('p', 'b'); //NonCompliant
    new RuntimeException("plop").getStackTrace()[0].getClassName(); //NonCompliant
  }


}