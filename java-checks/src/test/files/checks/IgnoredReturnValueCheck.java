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
    Integer.valueOf("1").byteValue(); // Noncompliant {{The return value of "byteValue" must be used.}}
    "plop".replace('p', 'b'); // Noncompliant {{The return value of "replace" must be used.}}
    new RuntimeException("plop").getStackTrace()[0].getClassName(); // Noncompliant {{The return value of "getClassName" must be used.}}
  }


}