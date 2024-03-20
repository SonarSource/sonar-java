package checks;

interface RedundantModifierInterface {
  void method1();
  public void method2(); // Noncompliant [[sc=3;ec=9]] {{"public" is redundant in this context.}}
  abstract void method3(); // Noncompliant {{"abstract" is redundant in this context.}}
  int field1 = 1;
  public int field2 = 1; // Noncompliant
  static int field3 = 1; // Noncompliant
  final int field4 = 1; // Noncompliant {{"final" is redundant in this context.}}


  static interface InnerInterface {}  // Noncompliant [[sc=3;ec=9]] {{"static" is redundant in this context.}}
  public interface InnerInterface2 {}  // Noncompliant [[sc=3;ec=9]] {{"public" is redundant in this context.}}
  static final class InnerClass {} // Noncompliant {{"static" is redundant in this context.}}
  public final class InnerClass2 {} // Noncompliant {{"public" is redundant in this context.}}
}

public @interface RedundantModifierCheckSample {
  String method1();
  public String method2(); // Noncompliant
  // Noncompliant@+1
  public static class InnerClass {} // Noncompliant
}

@interface RedundantModifierAnnotation {
  String method1();
  public String method2(); // Noncompliant
  // Noncompliant@+1
  public static interface InnerInterface {}  // Noncompliant
}

final class RedundantModifierClassFinal {
  static {

  }
  private RedundantModifierClassFinal() { // Compliant

  }
  void method1() {}
  final void method2() {} // Noncompliant
  final int field = 1;
  final class InnerClass {}
}

class RedundantModifierNonFinalClass {
  final void method2() {}
  public static interface InnerInterface {}  // Noncompliant {{"static" is redundant in this context.}}
  public static final class NestedClass {}
}
enum RedundantModifierFoo {
  RedundantModifierFoo("");

  private RedundantModifierFoo(String s) {} // Noncompliant {{"private" is redundant in this context.}}
}
enum RedundantModifierFoo2 {
  RedundantModifierFoo2("");

  RedundantModifierFoo2(String s) {}
  public static interface InnerInterface {} // Noncompliant {{"static" is redundant in this context.}}
  public static final class NestedClass {}
}

final record RedundantModifierRecord() { // Noncompliant {{"final" is redundant in this context.}}
  void foo() { }
}

record RedundantModifierRecord2() {
  final void foo() { } // Noncompliant {{"final" is redundant in this context.}}
}
