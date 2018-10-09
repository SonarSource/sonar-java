import org.junit.Ignore;
import org.junit.Assume;
import org.junit.jupiter.api.Disabled;

abstract class MyTest {
  @org.junit.Ignore
  void foo() {} // Noncompliant [[sc=8;ec=11]] {{Fix or remove this skipped unit test}}

  @Ignore
  void bar() {} // Noncompliant

  @Disabled
  void disabledJunit5() {} // Noncompliant

  void qix() {}

  @org.junit.Ignore("withComment") // compliant : explicit comment about why this test is ignored.
  void foo2() {}

  @Disabled("withComment") // compliant : explicit comment about why this test is ignored.
  void disabledJunit5WithComment() {}

  void assume1() {
    Assume.assumeTrue(false); // Noncompliant
    Assume.assumeTrue(true);
  }
  void assume2() {
    Assume.assumeFalse(true); // Noncompliant
    Assume.assumeFalse(false);
  }
  void assume3() {
    if(something) {
      Assume.assumeFalse(true); // compliant, we only detect basic cases
    }
  }

  void assume4() {
    System.out.println("foo");
    Assume.assumeFalse(true); // Noncompliant
  }
  void assume5() {
    Assume.assumeFalse(something); // compliant, unresolved expression
  }

  void assume6() {
    Assume.assumeFalse(Boolean.TRUE); // Noncompliant
  }

  public MyTest() {
    Assume.assumeTrue(false); // Noncompliant
  }

  abstract void assume7();

  void assume8(boolean var) {
    Assume.assumeFalse(var); // compliant, "var" is unknown
  }

}
