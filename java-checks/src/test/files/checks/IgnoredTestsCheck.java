import org.junit.Ignore;

class MyTest {

  @org.junit.Ignore
  void foo() {} //NonCompliant

  @Ignore
  void bar() {} //NonCompliant

  void qix() {}
}