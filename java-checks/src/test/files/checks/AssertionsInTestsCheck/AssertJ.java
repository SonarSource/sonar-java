import org.assertj.core.api.Assertions;
import org.assertj.core.api.Fail;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class AssertionsInTestsCheckAssertJ {

  private final SoftAssertions soft_assert = new SoftAssertions();

  @Rule
  public final JUnitSoftAssertions soft_assert_rule = new JUnitSoftAssertions();

  @Test
  public void contains_no_assertions() { // Noncompliant
  }

  @Test
  public void soft_assertThat() {
    soft_assert.assertThat(5).isLessThan(3);
  }

  @Test
  public void soft_assertAll() {
    soft_assert.assertAll();
  }

  @Test
  public void soft_assert_rule_assertThat() {
    soft_assert_rule.assertThat(5).isLessThan(3);
  }

  @Test
  public void assertions_assertThat() {
    Assertions.assertThat(true);
  }

  @Test
  public void assertions_assertThat_method_ref() {
    new java.util.ArrayList<Boolean>().forEach(Assertions::assertThat);
  }

  @Test
  public void assertions_fail() {
    Assertions.fail("a");
  }

  @Test
  public void assertions_fail_exception() {
    Assertions.fail("a", new IllegalArgumentException());
  }

  @Test
  public void assertions_fail_because() {
    Assertions.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
  }

  @Test
  public void fail_fail() {
    Fail.fail("failure");
  }

  @Test
  public void fail_fail_because() {
    Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
  }

  @Test
  public void fail_shouldHaveThrown() {
    Fail.shouldHaveThrown(IllegalArgumentException.class);
  }

  @Test
  public void assertion_in_helper_method() {
    helper_method(true);
  }

  @Test
  public void assertion_in_static_helper_method() {
    static_helper_method();
  }

  @Test
  public void assertion_in_helper_method_as_reference() {
    new java.util.ArrayList<Boolean>().forEach(this::helper_method);
    new java.util.ArrayList<Boolean>().forEach(this::helper_method_no_assert);
  }

  @Test
  public void no_assertion_in_helper_method() { // Noncompliant
    helper_method_no_assert(true);
  }

  @Test
  public void no_assertion_in_helper_method_as_reference() { // Noncompliant
    new java.util.ArrayList<Boolean>().forEach(this::helper_method_no_assert);
  }

  @Test
  public void assertion_in_external_static_method() { // Noncompliant
    // FP as rule currently cannot resolve cross-files custom assert methods
    org.sonarsource.helper.AssertionsHelper.customAssertion();
  }

  public void helper_method(boolean expected) {
    Assertions.assertThat(expected);
  }

  public static void static_helper_method() {
    new java.util.ArrayList<Boolean>().forEach(Assertions::assertThat);
    new java.util.ArrayList<Boolean>().forEach(java.util.Objects::isNull);
  }

  public void helper_method_no_assert(boolean expected) {
    new java.util.ArrayList<Boolean>().forEach(java.util.Objects::isNull);
  }

}
