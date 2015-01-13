import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

interface MyInterface {
  void method1();
  public void method2(); // Noncompliant
  abstract void method3(); // Noncompliant
  int field1 = 1;
  public int field2 = 1; // Noncompliant
  static int field3 = 1; // Noncompliant
  final int field4 = 1; // Noncompliant
}

public @interface MyAnnotation {
  String method2();
  public String method2(); // Noncompliant
}

final class MyClass {
  void method1() {}
  final void method2() {} // Noncompliant
  final int field = 1;
  final class InnerClass {}
}

class NonFinalClass {
  final void method2() {}
}
