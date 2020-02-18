package checks;

class TooManyMethodsCheckOnlyPublic { // Noncompliant {{class "TooManyMethodsCheckOnlyPublic" has 5 public methods, which is greater than the 4 authorized. Split it into smaller classes.}}
  public void method1() {}
  public void method2() {}
  public void method3() {}
  public void method4() {}
  public void method5() {}
  void method6() {}
}

class TooManyMethodsCheckOnlyPublicB {
  public void method1() {}
  public void method2() {}
  public void method3() {}
  public void method4() {}
  void method5() {}
}
