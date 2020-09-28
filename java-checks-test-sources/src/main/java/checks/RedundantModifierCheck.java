package checks;

interface RedundantModifierInterface {
  void method1();
  public void method2(); // Noncompliant [[sc=3;ec=9]] {{"public" is redundant in this context.}}
  abstract void method3(); // Noncompliant {{"abstract" is redundant in this context.}}
  int field1 = 1;
  public int field2 = 1; // Noncompliant
  static int field3 = 1; // Noncompliant
  final int field4 = 1; // Noncompliant {{"final" is redundant in this context.}}
}

public @interface RedundantModifierCheck {
  String method1();
  public String method2(); // Noncompliant
}

@interface RedundantModifierAnnotation {
  String method1();
  public String method2(); // Noncompliant
}

final class RedundantModifierClassFinal {
  void method1() {}
  final void method2() {} // Noncompliant
  final int field = 1;
  final class InnerClass {}
}

class RedundantModifierNonFinalClass {
  final void method2() {}
}
enum RedundantModifierFoo {
  RedundantModifierFoo("");

  private RedundantModifierFoo(String s) {} // Noncompliant {{"private" is redundant in this context.}}
}
enum RedundantModifierFoo2 {
  RedundantModifierFoo2("");

  RedundantModifierFoo2(String s) {}
}
