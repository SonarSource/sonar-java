package org.foo;

import Junit5ParameterizedTest.A;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class Junit4ParameterizedTest {

  @Parameters(name = "{index}: {0}+{1} = {2}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
      {0, 1, 1}, {1, 1, 2}, {2, 1, 3}, {3, 2, 5}, {4, 3, 7}, {5, 5, 10}, {6, 8, 14}
    });
  }

  @Parameter(0)
  public int a;

  @Parameter(1)
  public int b;

  @Parameter(2)
  public int c;

  @Test
  public void test() {
    assertEquals(a + b, c);
  }

  @Test
  public void myNormalJunit4Test() {
    assertThat(new Object()).isNotNull();
  }
}
