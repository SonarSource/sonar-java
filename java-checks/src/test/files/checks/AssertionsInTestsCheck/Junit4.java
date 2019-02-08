import java.util.List;
import javax.annotation.Nullable;
import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public abstract class AssertionsInTestsCheckJunit4 {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public org.junit.rules.ErrorCollector errorCollector = new org.junit.rules.ErrorCollector();

  private static int VAL = static_and_not_a_unit_test();

  private static int static_and_not_a_unit_test() {
    return 0;
  }

  @Test
  public void contains_no_assertions() { // Noncompliant [[sc=15;ec=37]] {{Add at least one assertion to this test case.}}
  }

  @Nullable
  public Test not_a_unit_test() {
    contains_no_assertions();
    return null;
  }

  @Test(timeout = 0L)
  public void contains_no_assertions_but_exceptions() { // Noncompliant
    throw new IllegalStateException("message");
  }

  @Test
  public abstract void abstract_unit_test();

  @Test
  public void junit_assert_equals() {
    org.junit.Assert.assertEquals(true, true);
  }

  @Test
  public void junit_assert_true() {
    org.junit.Assert.assertTrue(true);
    org.junit.Assert.assertTrue(true); // Coverage
  }

  @Test
  public void junit_assert_that() {
    org.junit.Assert.assertThat("aaa", org.junit.matchers.JUnitMatchers.containsString("a"));
  }

  @Test
  public void junit_assert_that_generic() {
    org.junit.Assert.<String>assertThat("aaa", org.junit.matchers.JUnitMatchers.containsString("a"));
  }

  @Test
  public void fest_assert_fail() {
    org.fest.assertions.Fail.fail("foo");
  }

  @Test
  public void fest_assert_method_reference() {
    new java.util.ArrayList<org.fest.assertions.GenericAssert>().forEach(org.fest.assertions.GenericAssert::isNull);
  }

  @Test
  public void fest_assert_helper_method() {
    helper_fest_assert();
  }

  @Test
  public void fest_assert_helper_method_reference() {
    helper_fest_assert_method_reference();
  }

  public void helper_fest_assert() {
    org.fest.assertions.Fail.fail("foo");
  }

  public void helper_fest_assert_method_reference() {
    new java.util.ArrayList<org.fest.assertions.GenericAssert>().forEach(org.fest.assertions.GenericAssert::isNull);
  }

  @Test
  public void fest_assert_that() {
    org.fest.assertions.Assertions.assertThat(true);
  }

  @Test
  public void fest_assert_that_equals() {
    org.fest.assertions.Assertions.assertThat(true).isEqualTo(true);
  }

  @Test
  public void junit_rule_expected_exception() {
    thrown.expect(IllegalStateException.class);
    throw new IllegalStateException("message");
  }

  @Test
  public void junit_rule_expected_exception_message() {
    thrown.expectMessage("message");
    throw new IllegalStateException("message");
  }

  @Test
  public void junit_rule_error_collector() { // Compliant
    errorCollector.checkThat("123", org.hamcrest.CoreMatchers.equalTo("123"));
  }

  @Test(expected = IllegalStateException.class)
  public void junit_test_annotated_with_expected() {
    throw new IllegalStateException("message");
  }

  @Test
  public void mockito_assertion_verify() {
    Mockito.verify(Mockito.mock(List.class)).clear();
  }

  @Test
  public void mockito_assertion_verify_times() {
    Mockito.verify(Mockito.mock(List.class), Mockito.times(0));
  }

  @Test
  public void mockito_assertion_verify_zero_interactions() {
    Mockito.verifyZeroInteractions(Mockito.mock(List.class));
  }

  @Test
  public void mockito_assertion_verify_no_more_interactions() {
    Mockito.verifyNoMoreInteractions(Mockito.mock(List.class));
  }

  static abstract class AbstractTest {
    @Test
    public abstract void unit_test();
  }

  static class ImplTest extends AbstractTest {
    @Override
    public void unit_test() { // Noncompliant
      // overridden test
    }
  }

  static class ExtendsTestCase extends TestCase {

    public void test_contains_no_assertions() { // Noncompliant
    }

    public void test_assertEquals() {
      assertEquals(true, true);
    }

    public void test_fail() {
      fail("message");
    }

  }

}
