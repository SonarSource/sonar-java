package checks.tests;


import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class NoJUnit4AssertionsInJUnit5TestsCheck_JUnit4SampleTest {

  // We do not raise in old JUnit 4 tests.

  @Test
  public void testOne() {
    List<Integer> list = List.of(1, 2, 3);
    assertEquals(3, list.size());
  }

  @Test
  public void testTwo() {
    fail();
  }
}
