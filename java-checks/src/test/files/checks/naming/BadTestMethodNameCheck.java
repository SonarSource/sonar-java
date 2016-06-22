import org.junit.Test;

class ATest {
  @Test
  void foo() {} // Noncompliant {{Rename this method name to match the regular expression: '^test[A-Z][a-zA-Z0-9]*$'}}

  @Test
  void testFoo() {} // Compliant

  void bar() {}
}

class BTest extends ATest {
  @Test
  @Override
  void foo() {} // Compliant
}
