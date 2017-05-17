import org.junit.Ignore;

class MyTest {

  @org.junit.Ignore
  void foo() {} // Noncompliant [[sc=8;ec=11]] {{Fix or remove this skipped unit test}}

  @Ignore
  void bar() {} // Noncompliant

  void qix() {}

  @org.junit.Ignore("withComment") // compliant : explicit comment about why this test is ignored.
  void foo2() {}
}
