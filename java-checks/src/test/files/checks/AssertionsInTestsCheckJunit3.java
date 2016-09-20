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

}
