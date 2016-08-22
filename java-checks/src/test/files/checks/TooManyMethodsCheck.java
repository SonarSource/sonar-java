import java.lang.Class;
import java.lang.Object;

class A { // Noncompliant [[sc=7;ec=8;secondary=5,6,7,8,9,10]] {{class "A" has 6 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
  A () {}
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
  void method5() {}
}

class A1 {
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
}

enum B { // Noncompliant {{enum "B" has 5 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
  A;
  void method1() {}
  public void method2() {}
  void method3() {}
  public void method4() {}
  public void method5() {}
}

interface C { // Noncompliant {{interface "C" has 5 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
  void method1();
  public void method2();
  void method3();
  public void method4();
  public void method5();
}

class E {
  C c = new C() { // Noncompliant {{Anonymous class "C" has 10 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
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

  C c2 = new C() { // compliant : only overriden methods
    public void method1() {}
    public void method2() {}
    public void method3() {}
    public void method4() {}
    public void method5() {}
  };
}

@interface D { // Noncompliant {{interface "D" has 5 methods, which is greater than the 4 authorized. Split it into smaller classes.}}
  String method1();
  public String method2();
  String method3();
  public String method4();
  public String method5();
}
