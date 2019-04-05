import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Fail;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.BDDAssertions;
import org.junit.Rule;
import org.junit.Test;

public abstract class AssertionsInTestsCheckAssertJ {

  interface Visitor {
    void visit(Object object);
  }

  class VisitorHandler {
    VisitorHandler(Object object, Visitor visitor) {
      visitor.visit(object);
    }
  }

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
  public void assertions_in_anonymous_class() {
    new VisitorHandler(null, new Visitor() {
      @Override
      public void visit(Object object) {
        Assertions.fail("a");
      }
    });
  }

  @Test
  public void no_assertion_in_anonymous_class() { // Noncompliant
    new VisitorHandler(null, new Visitor() {
      @Override
      public void visit(Object object) {
        java.util.Objects.isNull(object);
      }
    });
  }

  @Test
  public void assertions_in_lambda() {
    new VisitorHandler(null, object -> org.assertj.core.api.Assertions.fail("a"));
  }

  @Test
  public void assertion_in_helper_method() {
    helper_method(true);
  }

  @Test
  public void assertion_in_helper_method_chain() {
    intermediate_helper_method(true);
  }

  @Test
  public void assertion_in_helper_method() {
    helper_method_with_custom_assertion_method();
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

  @Test
  public void assertion_in_external_method() { // Noncompliant
    // FP as rule currently cannot resolve cross-files custom assert methods
    new org.sonarsource.helper.AssertionsHelper().customInstanceAssertion();
  }

  @Test
  public void assertion_in_whitelisted_external_static_method() {
    org.sonarsource.helper.AssertionsHelper.customAssertionAsRuleParameter(true);
  }

  @Test
  public void assertion_in_whitelisted_external_method() {
    new org.sonarsource.helper.AssertionsHelper().customInstanceAssertionAsRuleParameter();
  }

  @Test
  public void assertion_in_whitelisted_external_method_as_reference() {
    new java.util.ArrayList<Boolean>().forEach(org.sonarsource.helper.AssertionsHelper::customAssertionAsRuleParameter);
  }

  @Test
  public void assertion_method_reference_in_helper_method() {
    helper_method_with_custom_assertion_method_reference();
  }

  @Test
  public void assertion_in_constructor() {
    new org.sonarsource.helper.AssertionsHelper.ConstructorAssertion();
  }

  @Test
  public void assertion_from_unknown_symbol() {
    org.sonarsource.unknown.symbol.UnknownClass.assertSomething();
  }

  abstract boolean booleanMethod();
  abstract Object[] arrayMethod();
  abstract java.util.List<String> listStringMethod();

  @Test
  public void bdd_assertions_with_boolean() { // Compliant
    BDDAssertions.then(booleanMethod()).isTrue();
  }

  @Test
  public void bdd_assertions_with_array_assert() { // Cmpliant
    BDDAssertions.then(arrayMethod()).contains("A", "B");
  }

  @Test
  public void bdd_assertions_with_list() { // Compliant
    BDDAssertions.then(listStringMethod())
      .contains("Bob", "Alice")
      .doesNotContain("Maurice");
  }

  @Test
  public void bdd_assertions_split_with_intermediate_assertion() { // Compliant
    AbstractListAssert<?, ? extends List<? extends String>, String> thenResult = BDDAssertions.then(listStringMethod());
    thenResult
      .contains("Bob", "Alice")
      .doesNotContain("Maurice");
  }

  @Test
  public void bdd_assertions_example_without_assertion() { // Noncompliant - nothing is asserted here
    BDDAssertions.then(listStringMethod());
  }

  @Test
  public void assertion_in_constructor_from_helper_method() {
    helper_with_custom_constructor_assertion();
  }

  public void intermediate_helper_method(boolean booleanValue) {
    if (booleanValue) {
      intermediate_helper_method(!booleanValue);
    }
    helper_method(booleanValue);
  }

  public void helper_method(boolean expected) {
    Assertions.assertThat(expected);
  }

  public void helper_method_with_custom_assertion_method() {
    org.sonarsource.helper.AssertionsHelper.customAssertionAsRuleParameter(true);
  }

  public void helper_method_with_custom_assertion_method_reference() {
    new java.util.ArrayList<Boolean>().forEach(org.sonarsource.helper.AssertionsHelper::customAssertionAsRuleParameter);
  }

  public static void static_helper_method() {
    new java.util.ArrayList<Boolean>().forEach(Assertions::assertThat);
    new java.util.ArrayList<Boolean>().forEach(java.util.Objects::isNull);
  }

  public static void helper_with_custom_constructor_assertion() {
    new org.sonarsource.helper.AssertionsHelper.ConstructorAssertion();
  }

  public void helper_method_no_assert(boolean expected) {
    new java.util.ArrayList<Boolean>().forEach(java.util.Objects::isNull);
  }

}
