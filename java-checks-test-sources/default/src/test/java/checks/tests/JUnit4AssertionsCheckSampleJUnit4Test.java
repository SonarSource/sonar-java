package checks.tests;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JUnit4AssertionsCheckSampleJUnit4Test {

  // We do not raise in old JUnit 4 tests.

  @Test
  public void testOne() {
    assertEquals(2, 1 + 1); // Compliant
    System.out.println();
  }

  @Test
  public void testTwo() {
    fail();
  }
}
