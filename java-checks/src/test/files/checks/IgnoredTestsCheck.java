import org.junit.Ignore;

class MyTest {

  @org.junit.Ignore
  void foo() {} // Noncompliant {{Fix or remove this skipped unit test}}

  @Ignore
  void bar() {} // Noncompliant

  void qix() {}
}
