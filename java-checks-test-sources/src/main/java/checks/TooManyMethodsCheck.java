package checks;

class TooManyMethodsCheck { // Noncompliant [[sc=7;ec=26;secondary=+1,+2,+3,+4,+5,+6]] {{class "TooManyMethodsCheck" has 6 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
  TooManyMethodsCheck () {}
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
  void method5() {}
}

record TooManyMethodsRecord() { // Noncompliant [[sc=8;ec=28;secondary=+1,+2,+3,+4,+5,+6]] {{record "TooManyMethodsRecord" has 6 methods, which is greater than the 4 authorized. Split it into smaller records.}}
  TooManyMethodsRecord {}
  void m1() {}
  void m2() {}
  void m3() {}
  void m4() {}
  void m5() {}
}

class TooManyMethodsCheckA {
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
}

enum TooManyMethodsCheckB { // Noncompliant {{enum "TooManyMethodsCheckB" has 5 methods, which is greater than the 4 authorized. Split it into smaller enums.}}
  TooManyMethodsCheckA;
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
  public void method5() {}
}

interface TooManyMethodsCheckC { // Noncompliant {{interface "TooManyMethodsCheckC" has 5 methods, which is greater than the 4 authorized. Split it into smaller interfaces.}}
  void method1();
  void method2();
  void method3();
  void method4();
  void method5();
}

class TooManyMethodsCheckE {
  TooManyMethodsCheckC c = new TooManyMethodsCheckC() { // Noncompliant {{Anonymous class "TooManyMethodsCheckC" has 10 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
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

  TooManyMethodsCheckC c2 = new TooManyMethodsCheckC() { // compliant : only overriden methods, some without annotation
    /* @Override */ public void method1() {}
    /* @Override */ public void method2() {}
    @Override public void method3() {}
    @Override public void method4() {}
    @Override public void method5() {}
  };
}

@interface TooManyMethodsCheckD { // Noncompliant {{interface "TooManyMethodsCheckD" has 5 methods, which is greater than the 4 authorized. Split it into smaller interfaces.}}
  String method1();
  public String method2();
  String method3();
  public String method4();
  public String method5();
}
