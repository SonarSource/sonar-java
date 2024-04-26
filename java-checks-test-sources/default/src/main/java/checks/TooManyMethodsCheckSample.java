package checks;

class TooManyMethodsCheckSample { // Noncompliant {{class "TooManyMethodsCheckSample" has 6 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^
  TooManyMethodsCheckSample () {}
//  ^^^<
  void method1() {}
//  ^^^<
  public void method2() {}
//  ^^^<
  void method3() {}
//  ^^^<
  public void method4() {}
//  ^^^<
  void method5() {}
//  ^^^<
}

record TooManyMethodsRecord() { // Noncompliant {{record "TooManyMethodsRecord" has 6 methods, which is greater than the 4 authorized. Split it into smaller records.}}
//     ^^^^^^^^^^^^^^^^^^^^
  TooManyMethodsRecord {}
//  ^^^<
  void m1() {}
//  ^^^<
  void m2() {}
//  ^^^<
  void m3() {}
//  ^^^<
  void m4() {}
//  ^^^<
  void m5() {}
//  ^^^<
}

class TooManyMethodsCheckSampleA {
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
}

enum TooManyMethodsCheckSampleB { // Noncompliant {{enum "TooManyMethodsCheckSampleB" has 5 methods, which is greater than the 4 authorized. Split it into smaller enums.}}
  TooManyMethodsCheckSampleA;
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
  public void method5() {}
}

interface TooManyMethodsCheckSampleC { // Noncompliant {{interface "TooManyMethodsCheckSampleC" has 5 methods, which is greater than the 4 authorized. Split it into smaller interfaces.}}
  void method1();
  void method2();
  void method3();
  void method4();
  void method5();
}

class TooManyMethodsCheckSampleE {
  TooManyMethodsCheckSampleC c = new TooManyMethodsCheckSampleC() { // Noncompliant {{Anonymous class "TooManyMethodsCheckSampleC" has 10 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
    @Override public void method1() {}
    @Override public void method2() {}
    public void method3() {} // override, but without annotation
    public void method4() {} // override, but without annotation
    @Override public void method5() {}
    public void method6() {}
    public void method7() {}
    public void method8() {}
    public void method9() {}
    public void method10() {}
  };

  TooManyMethodsCheckSampleC c2 = new TooManyMethodsCheckSampleC() { // compliant : only overriden methods, some without annotation
    /* @Override */ public void method1() {}
    /* @Override */ public void method2() {}
    @Override public void method3() {}
    @Override public void method4() {}
    @Override public void method5() {}
  };
}

@interface TooManyMethodsCheckSampleD { // Noncompliant {{interface "TooManyMethodsCheckSampleD" has 5 methods, which is greater than the 4 authorized. Split it into smaller interfaces.}}
  String method1();
  public String method2();
  String method3();
  public String method4();
  public String method5();
}
