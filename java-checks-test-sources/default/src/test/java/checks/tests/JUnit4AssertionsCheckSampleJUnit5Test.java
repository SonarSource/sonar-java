package checks.tests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class JUnit4AssertionsCheckSampleJUnit5Test {

  // This JUnit 5 test uses new JUnit 5 assertions, so we do not raise.

  @Test
  void one() {
    assertEquals(2, 1 + 1);
    System.out.println();
    assertEquals(4, 2 + 2);
  }

  @Test
  void two() {
    System.out.println();
    fail();
  }
}
