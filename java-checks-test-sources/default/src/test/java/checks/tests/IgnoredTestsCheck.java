package checks.tests;

import org.junit.Ignore;
import org.junit.Assume;
import org.junit.jupiter.api.Disabled;

abstract class IgnoredTestsCheck {
  @org.junit.Ignore
  void foo() {} // Noncompliant [[sc=8;ec=11]] {{Either add an explanation about why this test is skipped or remove the "@Ignore" annotation.}}

  @Ignore
  void bar() {} // Noncompliant

  @Disabled
  void disabledJunit5() {} // Noncompliant [[sc=8;ec=22]] {{Either add an explanation about why this test is skipped or remove the "@Disabled" annotation.}}

  void qix() {}

  @org.junit.Ignore("withComment") // compliant : explicit comment about why this test is ignored.
  void foo2() {}

  @Ignore("withComment") // compliant : explicit comment about why this test is ignored.
  void foo2WithoutFQN() {}

  @Disabled("withComment") // compliant : explicit comment about why this test is ignored.
  void disabledJunit5WithComment() {}

  void assume1() {
    Assume.assumeTrue(false); // Noncompliant [[sc=12;ec=22;secondary=+0]] {{This assumption is called with a boolean constant; remove it or, to skip this test use an @Ignore/@Disabled annotation in combination with an explanation about why it is skipped.}}
    Assume.assumeTrue(true);
  }
  void assume2() {
    Assume.assumeFalse(true); // Noncompliant
    Assume.assumeFalse(false);
  }
  void assume3(boolean something) {
    if(something) {
      Assume.assumeFalse(true); // compliant, we only detect basic cases
    }
  }

  void assume4() {
    System.out.println("foo");
    Assume.assumeFalse(true); // Noncompliant
  }
  void assume5(boolean something) {
    Assume.assumeFalse(something); // compliant, unresolved expression
  }

  void assume6() {
    Assume.assumeFalse(Boolean.TRUE); // Noncompliant
  }

  public IgnoredTestsCheck() {
    Assume.assumeTrue(false); // Noncompliant
  }

  abstract void assume7();

  void assume8(boolean var) {
    Assume.assumeFalse(var); // compliant, "var" is unknown
  }

}
