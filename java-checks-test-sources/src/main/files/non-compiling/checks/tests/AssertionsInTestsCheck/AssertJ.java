package checks.tests.AssertionsInTestsCheck;

import org.junit.Test;

public abstract class AssertionsInTestsCheckAssertJ {

  @Test
  public void assertion_in_constructor() {
    new org.sonarsource.helper.AssertionsHelper.ConstructorAssertion();
  }

  @Test
  public void assertion_from_unknown_symbol() {
    org.sonarsource.unknown.symbol.UnknownClass.assertSomething();
  }

  @Test
  public void assertion_in_constructor_from_helper_method() {
    helper_with_custom_constructor_assertion();
  }

  public static void helper_with_custom_constructor_assertion() {
    new org.sonarsource.helper.AssertionsHelper.ConstructorAssertion();
  }

  @Test
  public void with_unknown_method_call() { // Compliant: unknown method calls are considered as assertions to avoid FP.
    unknownCall();
  }

}
