import junit.framework.TestCase;

import javax.annotation.Nullable;

public class AssertionsInTestsCheckJunit3 extends TestCase {

  public void test_contains_no_assertions() { // Noncompliant
  }

  public void test_assert_assertTrue() {
    org.junit.Assert.assertTrue(true);
  }

  @Nullable
  public void not_a_test() {
  }

  public void test_assertion_in_helper_method() {
    helper_method();
  }

  public void test_assertion_in_static_helper_method() {
    static_helper_method();
  }

  public void test_no_assertion_in_helper_method() { // Noncompliant
    helper_method_no_assert();
  }

  public void helper_method() {
    org.junit.Assert.assertTrue(true);
    org.junit.Assert.assertTrue(false);
  }

  public static void static_helper_method() {
    org.junit.Assert.assertTrue(true);
    org.junit.Assert.assertTrue(false);
  }

  public void helper_method_no_assert() {
    not_a_test();
  }

}
