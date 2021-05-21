package checks;

class TooManyMethodsCheckA { // Noncompliant [[sc=7;ec=27;secondary=+1,+2,+3,+4,+5,+6]] {{class "TooManyMethodsCheckA" has 6 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
  TooManyMethodsCheckA () {}
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
  void method5() {}
}

class TooManyMethodsCheckA1 {
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
}

enum TooManyMethodsCheckB { // Noncompliant {{enum "TooManyMethodsCheckB" has 5 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
  TooManyMethodsCheckA;
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
  public void method5() {}
}

interface TooManyMethodsCheckC { // Noncompliant {{interface "TooManyMethodsCheckC" has 5 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
  void method1();
  public void method2();
  void method3();
  public void method4();
  public void method5();
}

class TooManyMethodsCheckE {
  TooManyMethodsCheckC c = new TooManyMethodsCheckC() { // Noncompliant {{Anonymous class "TooManyMethodsCheckC" has 10 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
    public void method1() {}
    public void method2() {}
    public void method3() {}
    public void method4() {}
    public void method5() {}
    public void method6() {}
    public void method7() {}
    public void method8() {}
    public void method9() {}
    public void method10() {}
  };

  TooManyMethodsCheckC c2 = new TooManyMethodsCheckC() { // compliant : only overriden methods
    public void method1() {}
    public void method2() {}
    public void method3() {}
    public void method4() {}
    public void method5() {}
  };
}

@interface TooManyMethodsCheckD { // Noncompliant {{interface "TooManyMethodsCheckD" has 5 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
  String method1();
  public String method2();
  String method3();
  public String method4();
  public String method5();
}
