package checks.tests;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

class NoJUnit4AssertionsInJUnit5TestsCheck_JUnit5WithNoncompliantAssertionsSampleTest {
  @Test
  void good() {
    org.junit.jupiter.api.Assertions.assertTrue(true);
  }

  @Test
//^^^^^ >
  void testOne() {
    assertEquals(2, 1 + 1); // Noncompliant {{JUnit Jupiter tests should not use JUnit 4 assertions.}}
//  ^^^^^^^^^^^^
  }

  @Test
//^^^^^ >
  void testTwo() {
    helper();
    fail("message"); // Noncompliant {{JUnit Jupiter tests should not use JUnit 4 assertions.}}
//  ^^^^
  }

  private static void helper() {
  }

  @Test
  void testThree() {
    privateVerify();
  }

  private void privateVerify() {
    // FN, because we do not track usage across methods.
    fail("message");
  }
}
