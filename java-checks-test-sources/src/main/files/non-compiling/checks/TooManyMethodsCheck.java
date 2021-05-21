package checks;

class WithParentUnknown implements Unknown {
}

class TooManyMethodsCheck {
  WithParentUnknown withParentUnknown1 = new WithParentUnknown() { // Noncompliant - FP due to unknown hierarchy
    public void method1() {}
    public void method2() {}
    public void method3() {}
    public void method4() {}
    public void method5() {}
  };
}
