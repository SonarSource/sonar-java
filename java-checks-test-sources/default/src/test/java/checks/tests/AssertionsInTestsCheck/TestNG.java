package checks.tests.AssertionsInTestsCheck;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestNG {

  @Test
  public void contains_no_assertions() { // Noncompliant {{Add at least one assertion to this test case.}}
  }

  @Test
  public void with_assertion() {
    assertTrue(true);
  }

  @Ignore
  @Test
  public void ignored_test_without_assertion() { // Compliant - @Ignore skips the test
  }

  @Ignore
  @Test
  public void ignored_test_with_assertion() { // Compliant - @Ignore skips the test
    assertEquals(1, 1);
  }

  @Ignore
  public static class IgnoredTestClass {
    @Test
    public void test_without_assertion() { // Compliant - enclosing class is @Ignored
    }
  }

}