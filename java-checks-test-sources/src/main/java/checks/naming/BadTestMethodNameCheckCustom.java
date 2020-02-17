package checks.naming;

import org.junit.Test;

class ATest {
  @Test
  void foo() {} // Noncompliant {{Rename this method name to match the regular expression: '^test_sonar[A-Z][a-zA-Z0-9]*$'}}

  @Test
  void test_sonarFoo() {} // Compliant

  void bar() {}
}

class BTest extends ATest {
  @Test
  @Override
  void foo() {} // Compliant
}
